package org.labkey.targetedms.parser.skyaudit;

import java.nio.charset.Charset;
import java.util.List;

public class AuditLogMessage
{

    protected Integer _messageId;
    protected Integer _orderNumber;
    protected Integer _entryId;
    protected String _tag;
    protected String _messageType;
    protected String _enText;
    protected String _expandedText;
    protected String _reason;

    protected List<String> _names;


    public Integer getMessageId()
    {
        return _messageId;
    }

    public void setMessageId(Integer messageId)
    {
        _messageId = messageId;
    }

    public Integer getOrderNumber()
    {
        return _orderNumber;
    }

    public void setOrderNumber(Integer orderNumber)
    {
        _orderNumber = orderNumber;
    }

    public Integer getEntryId()
    {
        return _entryId;
    }

    public void setEntryId(Integer entryId)
    {
        _entryId = entryId;
    }

    public String getTag()
    {
        return _tag;
    }

    public void setTag(String tag)
    {
        _tag = tag;
    }

    public String getMessageType()
    {
        return _messageType;
    }

    public void setMessageType(String messageType)
    {
        _messageType = messageType;
    }

    public String getEnText()
    {
        return _enText;
    }

    public void setEnText(String enText)
    {
        _enText = enText;
    }

    public String getExpandedText()
    {
        return _expandedText;
    }

    public void setExpandedText(String exText)
    {
        _expandedText = exText;
    }

    public String getReason()
    {
        return _reason;
    }

    public List<String> getNames(){ return _names; }

    public byte[] getHashBytes(Charset cs){
        if(_enText != null)
            return _enText.getBytes(cs);
        else if(_expandedText != null)
            return _expandedText.getBytes(cs);
        else
            return _messageType.getBytes(cs);
    }

    @Override
    public String toString()
    {
        return "AuditLogMessage{" +
                "_orderNumber=" + _orderNumber +
                ", _entryId=" + _entryId +
                ", _tag='" + _tag + '\'' +
                ", _messageType='" + _messageType + '\'' +
                ", _enText='" + _enText + '\'' +
                ", _expandedText='" + _expandedText + '\'' +
                ", _reason='" + _reason + '\'' +
                ", _names=" + _names +
                '}';
    }
}
