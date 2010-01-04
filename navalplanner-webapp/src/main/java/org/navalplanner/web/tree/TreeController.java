/*
 * This file is part of ###PROJECT_NAME###
 *
 * Copyright (C) 2009 Fundación para o Fomento da Calidade Industrial e
 *                    Desenvolvemento Tecnolóxico de Galicia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.navalplanner.web.tree;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.navalplanner.business.trees.ITreeNode;
import org.navalplanner.web.common.IMessagesForUser;
import org.navalplanner.web.common.Level;
import org.navalplanner.web.common.MessagesForUser;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Tree;
import org.zkoss.zul.TreeModel;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

public abstract class TreeController<T extends ITreeNode<T>> extends
        GenericForwardComposer {

    private static final Log LOG = LogFactory.getLog(TreeController.class);

    private IMessagesForUser messagesForUser;

    private Component messagesContainer;

    protected Tree tree;

    protected TreeViewStateSnapshot snapshotOfOpenedNodes;

    private final Class<T> type;

    public abstract TreeitemRenderer getRenderer();

    protected TreeController(Class<T> type) {
        this.type = type;
    }

    public void indent() {
        if (tree.getSelectedCount() == 1) {
            indent(getSelectedNode());
        }
    }

    protected void indent(T element) {
        snapshotOfOpenedNodes = TreeViewStateSnapshot.snapshotOpened(tree);
        getModel().indent(element);
        filterByPredicateIfAny();
    }

    public TreeModel getTreeModel() {
        return (getModel() != null) ? getModel().asTree() : null;
    }

    protected abstract EntitiesTree<T> getModel();

    public void unindent() {
        if (tree.getSelectedCount() == 1) {
            unindent(getSelectedNode());
        }
    }

    protected void unindent(T element) {
        snapshotOfOpenedNodes = TreeViewStateSnapshot.snapshotOpened(tree);
        getModel().unindent(element);
        filterByPredicateIfAny();
    }

    public void up() {
        snapshotOfOpenedNodes = TreeViewStateSnapshot.snapshotOpened(tree);
        if (tree.getSelectedCount() == 1) {
            up(getSelectedNode());
        }
    }

    public void up(T element) {
        snapshotOfOpenedNodes = TreeViewStateSnapshot.snapshotOpened(tree);
        getModel().up(element);
        filterByPredicateIfAny();
    }

    public void down() {
        if (tree.getSelectedCount() == 1) {
            down(getSelectedNode());
        }
    }

    public void down(T element) {
        snapshotOfOpenedNodes = TreeViewStateSnapshot.snapshotOpened(tree);
        getModel().down(element);
        filterByPredicateIfAny();
    }

    protected T getSelectedNode() {
        return type.cast(tree.getSelectedItemApi().getValue());
    }

    public void move(Component dropedIn, Component dragged) {
        snapshotOfOpenedNodes = TreeViewStateSnapshot.snapshotOpened(tree);

        Treerow from = (Treerow) dragged;
        T fromNode = type.cast(((Treeitem) from.getParent()).getValue());
        if (dropedIn instanceof Tree) {
            getModel().moveToRoot(fromNode);
        }
        if (dropedIn instanceof Treerow) {
            Treerow to = (Treerow) dropedIn;
            T toNode = type.cast(((Treeitem) to.getParent()).getValue());

            getModel().move(fromNode, toNode);
        }
        filterByPredicateIfAny();
    }

    public void addElement() {
        snapshotOfOpenedNodes = TreeViewStateSnapshot.snapshotOpened(tree);
        try {
            if (tree.getSelectedCount() == 1) {
                getModel().addElementAt(getSelectedNode());
            } else {
                getModel().addElement();
            }
            filterByPredicateIfAny();
        } catch (IllegalStateException e) {
            LOG.warn("exception ocurred adding element", e);
            messagesForUser.showMessage(Level.ERROR, e.getMessage());
        }
    }

    protected abstract void filterByPredicateIfAny();

    protected static class TreeViewStateSnapshot {
        private final Set<Object> all;
        private final Set<Object> dataOpen;

        private TreeViewStateSnapshot(Set<Object> dataOpen, Set<Object> all) {
            this.dataOpen = dataOpen;
            this.all = all;
        }

        public static TreeViewStateSnapshot snapshotOpened(Tree tree) {
            Iterator<Treeitem> itemsIterator = tree.getTreechildrenApi()
                    .getItems().iterator();
            Set<Object> dataOpen = new HashSet<Object>();
            Set<Object> all = new HashSet<Object>();
            while (itemsIterator.hasNext()) {
                Treeitem treeitem = (Treeitem) itemsIterator.next();
                Object value = getAssociatedValue(treeitem);
                if (treeitem.isOpen()) {
                    dataOpen.add(value);
                }
                all.add(value);
            }
            return new TreeViewStateSnapshot(dataOpen, all);
        }

        private static Object getAssociatedValue(Treeitem treeitem) {
            return treeitem.getValue();
        }

        public void openIfRequired(Treeitem item) {
            Object value = getAssociatedValue(item);
            item.setOpen(isNewlyCreated(value) || wasOpened(value));
        }

        private boolean wasOpened(Object value) {
            return dataOpen.contains(value);
        }

        private boolean isNewlyCreated(Object value) {
            return !all.contains(value);
        }
    }

    public void removeElement() {
        Set<Treeitem> selectedItems = tree.getSelectedItems();
        for (Treeitem treeItem : selectedItems) {
            remove(type.cast(treeItem.getValue()));
        }
        filterByPredicateIfAny();
    }

    protected void remove(T element) {
        getModel().removeNode(element);
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        messagesForUser = new MessagesForUser(messagesContainer);
    }

    public boolean isItemSelected() {
        return tree.getSelectedItem() != null;
    }

    public boolean isNotItemSelected() {
        return !isItemSelected();
    }

    protected Button btnNew, btnDown, btnUp, btnUnindent, btnIndent, btnDelete;


    protected TreeViewStateSnapshot getSnapshotOfOpenedNodes() {
        return snapshotOfOpenedNodes;
    }

}
