/*
 * Copyright (c) 2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.targetedms.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.view.ActionURL;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.parser.ReplicateAnnotation;
import org.labkey.targetedms.parser.SampleFile;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * User: vsharma
 * Date: 5/3/12
 * Time: 9:10 PM
 */
public class ChromatogramDisplayColumnFactory implements DisplayColumnFactory
{
    private Container _container;
    private TYPE _type;
    private int _chartWidth;
    private int _chart_height;
    private boolean _syncY = false;
    private boolean _syncX = false;
    private String _annotationsFilter;
    private String _replicatesFilter;

    public static enum TYPE
    {
        PEPTIDE,
        PRECURSOR
        //TRANSITION
    }

    public ChromatogramDisplayColumnFactory(Container container, TYPE type)
    {
        this(container, type, 400, 400, false, false, null, null);
    }

    public ChromatogramDisplayColumnFactory(Container container, TYPE type,
                                            int chartWidth, int chartHeight,
                                            boolean syncIntensity, boolean syncMz, String annotationsFilter, String replicatesFilter)
    {
        _container = container;
        _type = type;
        _chartWidth = chartWidth;
        _chart_height = chartHeight;
        _syncY = syncIntensity;
        _syncX = syncMz;
        _annotationsFilter = annotationsFilter;
        _replicatesFilter = replicatesFilter;
    }

    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new DataColumn(colInfo) {
            @Override
            public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
            {
                Object Id = getValue(ctx);  // Primary key from the relevant table
                if(null == Id)
                    return;

                ActionURL chromAction = null;
                SampleFile sampleFile = null;
                switch (_type)
                {
                    case PEPTIDE:
                        chromAction = new ActionURL(TargetedMSController.PeptideChromatogramChartAction.class, _container);
                        sampleFile = ReplicateManager.getSampleFileForPeptideChromInfo((Integer) Id);
                        break;
                    case PRECURSOR:
                        chromAction = new ActionURL(TargetedMSController.PrecursorChromatogramChartAction.class, _container);
                        sampleFile = ReplicateManager.getSampleFileForPrecursorChromInfo((Integer) Id);
                        break;
//                    case TRANSITION:
//                        chromAction = new ActionURL(TargetedMSController.TransitionChromatogramChartAction.class, _container);
//                        sampleFile = ReplicateManager.getSampleFileForTransitionChromInfo((Integer) Id);
//                        break;
                }

                if (chromAction != null)
                {
                    chromAction.addParameter("id", String.valueOf(Id));
                    chromAction.addParameter("chartWidth", String.valueOf(_chartWidth));
                    chromAction.addParameter("chartHeight", String.valueOf(_chart_height));
                    chromAction.addParameter("syncY", String.valueOf(_syncY));
                    chromAction.addParameter("syncX", String.valueOf(_syncX));
                    chromAction.addParameter("annotationsFilter", String.valueOf(_annotationsFilter));
                    chromAction.addParameter("replicatesFilter", String.valueOf(_replicatesFilter));

                    String imgLink = "<img src=\""+chromAction.getLocalURIString()+"\" alt=\"Chromatogram "+sampleFile.getSampleName()+"\">";
                    out.write(imgLink);
                }
            }
        };
    }
}
