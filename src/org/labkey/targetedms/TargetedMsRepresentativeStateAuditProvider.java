/*
 * Copyright (c) 2013-2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.targetedms;

import org.labkey.api.audit.AbstractAuditTypeProvider;
import org.labkey.api.audit.AuditLogService;
import org.labkey.api.audit.AuditTypeEvent;
import org.labkey.api.audit.AuditTypeProvider;
import org.labkey.api.audit.query.AbstractAuditDomainKind;
import org.labkey.api.data.Container;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * User: klum
 * Date: 7/21/13
 */
public class TargetedMsRepresentativeStateAuditProvider extends AbstractAuditTypeProvider implements AuditTypeProvider
{
    public static final String AUDIT_EVENT_TYPE = "TargetedMSRepresentativeStateEvent";

    static final List<FieldKey> defaultVisibleColumns = new ArrayList<>();

    static {

        defaultVisibleColumns.add(FieldKey.fromParts(COLUMN_NAME_CREATED));
        defaultVisibleColumns.add(FieldKey.fromParts(COLUMN_NAME_CREATED_BY));
        defaultVisibleColumns.add(FieldKey.fromParts(COLUMN_NAME_IMPERSONATED_BY));
        defaultVisibleColumns.add(FieldKey.fromParts(COLUMN_NAME_PROJECT_ID));
        defaultVisibleColumns.add(FieldKey.fromParts(COLUMN_NAME_CONTAINER));
        defaultVisibleColumns.add(FieldKey.fromParts(COLUMN_NAME_COMMENT));
    }

    @Override
    protected AbstractAuditDomainKind getDomainKind()
    {
        return new TargetedMsRepresentativeStateAuditDomainKind();
    }

    @Override
    public String getEventName()
    {
        return AUDIT_EVENT_TYPE;
    }

    @Override
    public String getLabel()
    {
        return "TargetedMS Representative State Event";
    }

    @Override
    public String getDescription()
    {
        return "TargetedMS Representative State Event";
    }

    @Override
    public <K extends AuditTypeEvent> Class<K> getEventClass()
    {
        return (Class<K>)AuditTypeEvent.class;
    }

    public static void addAuditEntry(Container container, User user, String comment)
    {
        AuditTypeEvent event = new AuditTypeEvent(AUDIT_EVENT_TYPE, container.getId(), comment);

        event.setProjectId(container.getProject().getId());
        AuditLogService.get().addEvent(user, event);
    }

    @Override
    public List<FieldKey> getDefaultVisibleColumns()
    {
        return defaultVisibleColumns;
    }


    public static class TargetedMsRepresentativeStateAuditDomainKind extends AbstractAuditDomainKind
    {
        public static final String NAME = "TargetedMsRepresentativeStateAuditDomain";
        public static String NAMESPACE_PREFIX = "Audit-" + NAME;

        public TargetedMsRepresentativeStateAuditDomainKind()
        {
            super(AUDIT_EVENT_TYPE);
        }

        @Override
        public Set<PropertyDescriptor> getProperties()
        {
            return Collections.emptySet();
        }

        @Override
        protected String getNamespacePrefix()
        {
            return NAMESPACE_PREFIX;
        }

        @Override
        public String getKindName()
        {
            return NAME;
        }
    }
}
