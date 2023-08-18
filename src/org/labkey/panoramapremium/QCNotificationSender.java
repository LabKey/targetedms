package org.labkey.panoramapremium;

import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.security.ValidEmail;
import org.labkey.api.settings.LookAndFeelProperties;
import org.labkey.api.targetedms.ITargetedMSRun;
import org.labkey.api.targetedms.SkylineDocumentImportListener;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.targetedms.model.SampleFileInfo;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.ExceptionUtil;
import org.labkey.api.util.MailHelper;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.panoramapremium.model.UserSubscription;
import org.labkey.targetedms.TargetedMSManager;

import javax.mail.Message;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class QCNotificationSender implements SkylineDocumentImportListener
{
    private static final QCNotificationSender _instance = new QCNotificationSender();
    private static final int QC_SAMPLE_FILE_LIMIT = 5;

    public static QCNotificationSender get()
    {
        return _instance;
    }

    @Override
    public void onDocumentImport(Container container, User user, ITargetedMSRun run)
    {
        //check for QC folder
        if(TargetedMSService.get().getFolderType(container) == TargetedMSService.FolderType.QC)
        {
            //check if the user is subscribed to receive qc notifications
            List<UserSubscription> userSubscriptions = getUserSubscriptions(container);

            if (!userSubscriptions.isEmpty())
            {
                List<SampleFileInfo> sampleFiles = TargetedMSService.get().getSampleFiles(container, user, QC_SAMPLE_FILE_LIMIT);

                //sorting the map to show latest run first
                sampleFiles.sort(Comparator.comparing(SampleFileInfo::getAcquiredTime).reversed());

                userSubscriptions.forEach(userSubscription -> {
                    int totalOutliers = 0;
                    int totalOutlierSubscribed = userSubscription.getOutliers();
                    int sampleCount = 0;

                    List<SampleFileInfo> samplesToEmail = new ArrayList<>();

                    for (SampleFileInfo sampleFileInfoDetails : sampleFiles)
                    {
                        sampleCount++;

                        if (sampleFileInfoDetails.getLeveyJennings() > 0 || sampleFileInfoDetails.getmR() > 0 || sampleFileInfoDetails.getCUSUMm() > 0 || sampleFileInfoDetails.getCUSUMv() > 0)
                        {
                            totalOutliers += sampleFileInfoDetails.getLeveyJennings() + sampleFileInfoDetails.getmR() + sampleFileInfoDetails.getCUSUMm() + sampleFileInfoDetails.getCUSUMv();
                            samplesToEmail.add(sampleFileInfoDetails);
                        }

                        if(sampleCount == userSubscription.getSamples())
                        {
                            break;
                        }
                    }

                    //send email
                    if (totalOutliers >= totalOutlierSubscribed)
                    {
                        sendQCNotification(container, userSubscription, samplesToEmail);
                    }
                });
            }
        }
    }

    private static class EmailSubjectAndBody
    {
        int recentOutliers;
        String msgBody;

        public int getRecentOutliers()
        {
            return recentOutliers;
        }

        public void setRecentOutliers(int recentOutliers)
        {
            this.recentOutliers = recentOutliers;
        }

        public String getMsgBody()
        {
            return msgBody;
        }

        public void setMsgBody(String msgBody)
        {
            this.msgBody = msgBody;
        }
    }

    private void sendQCNotification(Container container, UserSubscription userSubscription, List<SampleFileInfo> samplesToEmail)
    {
        try
        {
            User recipient = UserManager.getUser(userSubscription.getUserId());
            ValidEmail ve = new ValidEmail(recipient.getEmail());
            EmailSubjectAndBody emailSubjectAndBody = buildMessage(container, recipient, samplesToEmail);

            MailHelper.MultipartMessage msg = MailHelper.createMultipartMessage();
            msg.setFrom(LookAndFeelProperties.getInstance(container).getSystemEmailAddress());
            msg.setSubject("Panorama QC Notification - " + emailSubjectAndBody.getRecentOutliers() + " outliers in new sample imported into " + container.getPath());
            msg.setRecipient(Message.RecipientType.TO, ve.getAddress());
            msg.setEncodedHtmlContent(emailSubjectAndBody.getMsgBody());
            MailHelper.send(msg, recipient, container);
        }
        catch (Exception e)
        {
            ExceptionUtil.logExceptionToMothership(null, e);
        }
    }

    private EmailSubjectAndBody buildMessage(Container container, User user, List<SampleFileInfo> samplesToEmail)
    {
        EmailSubjectAndBody emailSubjectAndBody = new EmailSubjectAndBody();
        StringBuilder html = new StringBuilder();

        html.append("<html><body>");

        html.append("<p><a href=\"").append(container.getStartURL(user)).append("\">View in Panorama</a></p>");

        //create separate table for each sample file having outliers
        for (SampleFileInfo sampleFileInfoDetails : samplesToEmail)
        {
            html.append("<h4>").append(PageFlowUtil.filter(sampleFileInfoDetails.getSampleFile())).append(" - ");

            var totalOutliers = sampleFileInfoDetails.getLeveyJennings() + sampleFileInfoDetails.getmR() + sampleFileInfoDetails.getCUSUMm() + sampleFileInfoDetails.getCUSUMv();
            if (totalOutliers > 0)
            {
                html.append(sampleFileInfoDetails.getLeveyJennings()).append("Levey-Jennings outliers, ");
                html.append(totalOutliers).append(" total outliers");
            }
            else
            {
                html.append("no outliers");
            }
            html.append("</h4>\n");
            html.append("<p>Acquired ").append(DateUtil.formatDateTime(container, sampleFileInfoDetails.getAcquiredTime())).append("</p>");
            html.append("<table style=\"border: 1px solid #d3d3d3;\"><thead><tr><td style=\"border: 1px solid #d3d3d3;\"></td><td  style=\"border: 1px solid #d3d3d3;\" colspan=\"7\" align=\"center\">Outliers</td></tr>")
                    .append("<tr>")
                    .append("<td style=\"border: 1px solid #d3d3d3;\"></td><td style=\"border: 1px solid #d3d3d3;\"></td>")
                    .append("<td style=\"border: 1px solid #d3d3d3;\"></td><td style=\"border: 1px solid #d3d3d3;\" colspan=\"4\" align=\"center\">CUSUM</td>")
                    .append("<td style=\"border: 1px solid #d3d3d3;\"></td>")
                    .append("</tr>")
                    .append("<tr>")
                    .append("<td style=\"border: 1px solid #d3d3d3;\">Metric</td>")
                    .append("<td style=\"border: 1px solid #d3d3d3;\">Levey-Jennings</td>")
                    .append("<td style=\"border: 1px solid #d3d3d3;\">Moving Range</td>")
                    .append("<td style=\"border: 1px solid #d3d3d3;\" title=\"Mean CUSUM-\">Mean-</td>")
                    .append("<td style=\"border: 1px solid #d3d3d3;\" title=\"Mean CUSUM+\">Mean+</td>")
                    .append("<td style=\"border: 1px solid #d3d3d3;\" title=\"Variability CUSUM-\">Variability-</td>")
                    .append("<td style=\"border: 1px solid #d3d3d3;\" title=\"Variability CUSUM+\">Variability+</td>")
                    .append("<td style=\"border: 1px solid #d3d3d3;\" title=\"Total\"><b>Total</b></td>")
                    .append("</tr>")
                    .append("</thead><tbody>");

            sampleFileInfoDetails.getByMetric().forEach((key, outlier) -> {
                var outlierTotal = outlier.getLeveyJennings() + outlier.getmR() + outlier.getCUSUMmN() + outlier.getCUSUMmP() + outlier.getCUSUMvN() + outlier.getCUSUMvP();
                html.append("<tr>")
                        .append("<td style=\"border: 1px solid #d3d3d3; text-align: right;\">").append(PageFlowUtil.filter(key)).append("</td>")
                        .append("<td style=\"border: 1px solid #d3d3d3; text-align: right;\">").append(outlier.getLeveyJennings() > 0 ? "<b>" : "").append(PageFlowUtil.filter(outlier.getLeveyJennings())).append("</td>").append(outlier.getLeveyJennings() > 0 ? "</b>" : "")
                        .append("<td style=\"border: 1px solid #d3d3d3; text-align: right;\">").append(outlier.getmR() > 0 ? "<b>" : "").append(PageFlowUtil.filter(outlier.getmR())).append("</td>").append(outlier.getmR() > 0 ? "</b>" : "")
                        .append("<td style=\"border: 1px solid #d3d3d3; text-align: right;\">").append(outlier.getCUSUMmN() > 0 ? "<b>" : "").append(PageFlowUtil.filter(outlier.getCUSUMmN())).append("</td>").append(outlier.getCUSUMmN() > 0 ? "</b>" : "")
                        .append("<td style=\"border: 1px solid #d3d3d3; text-align: right;\">").append(outlier.getCUSUMmP() > 0 ? "<b>" : "").append(PageFlowUtil.filter(outlier.getCUSUMmP())).append("</td>").append(outlier.getCUSUMmP() > 0 ? "</b>" : "")
                        .append("<td style=\"border: 1px solid #d3d3d3; text-align: right;\">").append(outlier.getCUSUMvN() > 0 ? "<b>" : "").append(PageFlowUtil.filter(outlier.getCUSUMvN())).append("</td>").append(outlier.getCUSUMvN() > 0 ? "</b>" : "")
                        .append("<td style=\"border: 1px solid #d3d3d3; text-align: right;\">").append(outlier.getCUSUMvP() > 0 ? "<b>" : "").append(PageFlowUtil.filter(outlier.getCUSUMvP())).append("</td>").append(outlier.getCUSUMvP() > 0 ? "</b>" : "")
                        .append("<td style=\"border: 1px solid #d3d3d3; text-align: right;\">").append("<b>").append(PageFlowUtil.filter(outlierTotal)).append("</b>").append("</td>");
                html.append("</tr>");
            });

            // Remember the total for the most recent import
            if (emailSubjectAndBody.getRecentOutliers() == 0)
            {
                emailSubjectAndBody.setRecentOutliers(sampleFileInfoDetails.getLeveyJennings() + sampleFileInfoDetails.getmR() + sampleFileInfoDetails.getCUSUMmN() + sampleFileInfoDetails.getCUSUMmP() + sampleFileInfoDetails.getCUSUMvN() + sampleFileInfoDetails.getCUSUMvP());
            }

            html.append("<tr>")
                    .append("<td style=\"border: 1px solid #d3d3d3;\"><b>Total</b></td>")
                    .append("<td style=\"border: 1px solid #d3d3d3; text-align: right;\">").append(sampleFileInfoDetails.getLeveyJennings() > 0 ? "<b>" : "").append(PageFlowUtil.filter(sampleFileInfoDetails.getLeveyJennings())).append("</td>").append(sampleFileInfoDetails.getLeveyJennings() > 0 ? "</b>" : "")
                    .append("<td style=\"border: 1px solid #d3d3d3; text-align: right;\">").append(sampleFileInfoDetails.getmR() > 0 ? "<b>" : "").append(PageFlowUtil.filter(sampleFileInfoDetails.getmR())).append("</td>").append(sampleFileInfoDetails.getmR() > 0 ? "</b>" : "")
                    .append("<td style=\"border: 1px solid #d3d3d3; text-align: right;\">").append(sampleFileInfoDetails.getCUSUMmN() > 0 ? "<b>" : "").append(PageFlowUtil.filter(sampleFileInfoDetails.getCUSUMmN())).append("</td>").append(sampleFileInfoDetails.getCUSUMmN() > 0 ? "</b>" : "")
                    .append("<td style=\"border: 1px solid #d3d3d3; text-align: right;\">").append(sampleFileInfoDetails.getCUSUMmP() > 0 ? "<b>" : "").append(PageFlowUtil.filter(sampleFileInfoDetails.getCUSUMmP())).append("</td>").append(sampleFileInfoDetails.getCUSUMmP() > 0 ? "</b>" : "")
                    .append("<td style=\"border: 1px solid #d3d3d3; text-align: right;\">").append(sampleFileInfoDetails.getCUSUMvN() > 0 ? "<b>" : "").append(PageFlowUtil.filter(sampleFileInfoDetails.getCUSUMvN())).append("</td>").append(sampleFileInfoDetails.getCUSUMvN() > 0 ? "</b>" : "")
                    .append("<td style=\"border: 1px solid #d3d3d3; text-align: right;\">").append(sampleFileInfoDetails.getCUSUMvP() > 0 ? "<b>" : "").append(PageFlowUtil.filter(sampleFileInfoDetails.getCUSUMvP())).append("</td>").append(sampleFileInfoDetails.getCUSUMvP() > 0 ? "</b>" : "")
                    .append("<td style=\"border: 1px solid #d3d3d3; text-align: right;\">").append( "<b>").append(PageFlowUtil.filter(totalOutliers)).append( "</b>").append("</td>");
            html.append("</tr>");

            html.append("</tbody></table><br/>");
        }


        html.append("</body></html>");
        emailSubjectAndBody.setMsgBody(html.toString());

        return emailSubjectAndBody;
    }

    public List<UserSubscription> getUserSubscriptions(Container container)
    {
        SQLFragment sql = new SQLFragment("SELECT userId,enabled, samples, outliers FROM ").
                append(TargetedMSManager.getTableInfoQCEmailNotifications(), "qcen").
                append(" WHERE Container = ? AND enabled = ?").
                add(container.getId()).
                add(true);
        DbSchema query = DbSchema.get("targetedms", DbSchemaType.Module);
        return new SqlSelector(query, sql).getArrayList(UserSubscription.class);
    }
 }
