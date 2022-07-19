/*
 * Copyright (c) 2019 LabKey Corporation
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
package org.labkey.targetedms.parser.skyaudit;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.util.GUID;
import org.labkey.api.util.logging.LogHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AuditLogTree implements Iterable<AuditLogTree>
{
    public static final String NULL_STRING = "(null)";

    private static final Logger LOG = LogHelper.getLogger(AuditLogTree.class, "Skyline audit log validation");

    private final Map<String, AuditLogTree> _children = new HashMap<>();
    private final String _entryHash;
    private final GUID _documentGUID;
    private final String _parentEntryHash;
    private final int _entryId;
    private Long _versionId;     //runId of the document if this record is the last in that document's log. Null otherwise.

    public AuditLogTree(int pEntryid, GUID pDocumentGUID, String pEntryHash, String pParentEntryHash, Long pVersionId)
    {
        _entryHash = pEntryHash;
        _documentGUID = pDocumentGUID;
        _parentEntryHash = pParentEntryHash;
        _entryId = pEntryid;
        _versionId = pVersionId;
    }

    // Only audit log entries with versions will have duplicate entryIds when multiple runs
    // claim same audit log entry, so concat version to the map key
    public static String getMapId(String entryId, Long versionId)
    {
        String id = entryId;
        if (null != versionId)
            id += "-" + versionId;

        return id;
    }

    public String getEntryHash()
    {
        return _entryHash;
    }

    public GUID getDocumentGUID()
    {
        return _documentGUID;
    }

    public String getParentEntryHash()
    {
        return _parentEntryHash;
    }

    public int getEntryId()
    {
        return _entryId;
    }

    public Long getVersionId()
    {
        return _versionId;
    }

    public AuditLogTree addChild(AuditLogTree pChild)
    {
        String id = getMapId(pChild.getEntryHash(), pChild.getVersionId());
        if(!_children.containsKey(id))
            _children.put(id, pChild);
        return pChild;
    }

    public boolean hasChild(String pEntryHash, Long versionId)
    {
        return _children.containsKey(getMapId(pEntryHash, versionId));
    }

    public boolean hasChildEntry(String pEntryHash)
    {
        return _children.keySet().stream().anyMatch(c -> c.startsWith(pEntryHash));
    }

    public AuditLogTree getChild(String pEntryHash, Long versionId) {
        return _children.get(getMapId(pEntryHash, versionId));
    }

    public AuditLogTree getChildEntry(String pEntryHash) {
        for (String key : _children.keySet())
        {
            if (key.startsWith(pEntryHash))
                return _children.get(key);
        }
        return null;
    }

    public int getTreeSize()
    {
        // Don't use call stack recursion to avoid StackOverflowErrors on large trees - issue 43980
        List<AuditLogTree> stack = new ArrayList<>();
        stack.add(this);

        int result = 1;

        while (!stack.isEmpty())
        {
            AuditLogTree current = stack.remove(0);
            result += current._children.size();
            stack.addAll(current._children.values());
        }

        return result;
    }

    /***
     * Finds log entries that belong to the given document version only and not to any other version.
     * This is useful when we need to delete the version and it's log, but some of the entries can be
     * shared with other versions
     * @param versionId  runId of the document version to be deleted
     * @return list of entries that can be deleted safely without corrupting other versions' logs.
     */
    public Set<AuditLogTree> deleteList(long versionId)
    {
        Map<AuditLogTree, AuditLogTree> toDelete = new HashMap<>();
        List<StackEntry> stack = new ArrayList<>();
        stack.add(new StackEntry(null, this));

        int index = 0;

        // Do a breadth-first traversal to build our stack. Don't use the call stack to avoid StackOverflowErrors - see issue 40693
        while (index < stack.size())
        {
            StackEntry stackEntry = stack.get(index++);
            for (AuditLogTree child : stackEntry._entry._children.values())
            {
                stack.add(new StackEntry(stackEntry._entry, child));
            }
        }

        Map<String, Integer> entityCount = new HashMap<>();

        // Now look at them in reverse order,
        for (int i = stack.size() - 1; i >= 0; i--)
        {
            StackEntry stackEntry = stack.get(i);
            AuditLogTree currentEntry = stackEntry._entry;

            Integer count = entityCount.get(currentEntry.getEntryHash());
            if (count != null)
            {
                entityCount.put(currentEntry.getEntryHash(), ++count);
            }
            else
            {
                entityCount.put(currentEntry.getEntryHash(), 1);
            }

            if (currentEntry._children.size() == 0)      //if this is a leaf
            {
                if (currentEntry._versionId == null)
                {
                    if (currentEntry._entryId != 0)                  //No need to check for the root node.
                    {
                        LOG.warn(String.format("Audit log entry with ID %d is a leaf but has no version ID. This might be a data corruption.", this._entryId));
                    }
                }
                else if (versionId == currentEntry._versionId)      //check if it is the right version id
                {
                    toDelete.put(currentEntry, stackEntry._parent);
                }
            }
        }

        for (Map.Entry<AuditLogTree, AuditLogTree> treeEntry : toDelete.entrySet())
        {
            // Don't delete anything used in multiple contexts
            if (entityCount.get(treeEntry.getKey().getEntryHash()) > 1)
            {
                toDelete.remove(treeEntry.getKey());
            }
            else
            {
                AuditLogTree parent = treeEntry.getValue();
                if (parent != null)
                {
                    parent._children.remove(treeEntry.getKey().getEntryHash());
                    // Check if the parent is now the last hop in the chain
                    if (parent._children.isEmpty() && parent._versionId == null)
                    {
                        parent._versionId = versionId;
                    }
                }
            }
        }

        return toDelete.keySet();
    }

    /** For traversing the tree of audit entries, we need to hold both the parent and the child in the relationship */
    private static class StackEntry
    {
        private final AuditLogTree _parent;
        private final AuditLogTree _entry;

        public StackEntry(AuditLogTree parent, AuditLogTree entry)
        {
            _parent = parent;
            _entry = entry;
        }
    }

    public AuditLogTree findVersionEntry(long pVersionId)
    {
        // Don't use call stack recursion to avoid StackOverflowErrors on large trees - issue 43980
        List<AuditLogTree> stack = new ArrayList<>();
        stack.add(this);

        while (!stack.isEmpty())
        {
            AuditLogTree currentEntry = stack.remove(0);
            // check if it is the right version id
            if (currentEntry._versionId != null && currentEntry._versionId == pVersionId)
            {
                return currentEntry;
            }

            // performing depth-first search because audit log trees are typically deeper than wider.
            stack.addAll(0, currentEntry._children.values());
        }
        return null;
    }

    @Override
    @NotNull
    public Iterator<AuditLogTree> iterator()
    {
        return _children.values().iterator();
    }

    @Override
    public int hashCode()
    {
        if (this._entryHash != null)
            return this._entryHash.hashCode();
        else
            return super.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof AuditLogTree)
        {
            if(this._entryHash == null) return false;
            return (this._entryHash.equals( ((AuditLogTree) o)._entryHash) );
        }
        else
            return false;
    }

}
