/*
 * Copyright (C) 2010 Teleal GmbH, Switzerland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.teleal.cling.support.contentdirectory.ui;

import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.controlpoint.ControlPoint;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.support.model.container.Container;

import javax.swing.JTree;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

/**
 * Ready-to-use JTree with interactive browsing of a backend <em>ContentDirectory</em> service.
 * <p>
 * Shows the loading status as icon + text informational node directly in the tree.
 * </p>
 *
 * @author Christian Bauer
 */
public abstract class ContentTree extends JTree implements ContentBrowseActionCallbackCreator {

    final protected Container rootContainer;
    final protected DefaultMutableTreeNode rootNode;

    public ContentTree(ControlPoint controlPoint, Service service) {

        rootContainer = createRootContainer(service);
        rootNode = new DefaultMutableTreeNode(rootContainer) {
            @Override
            public boolean isLeaf() {
                return false;
            }
        };

        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        setModel(treeModel);

        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        addTreeWillExpandListener(createContainerTreeExpandListener(controlPoint, service, treeModel));
        setCellRenderer(createContainerTreeCellRenderer());

        controlPoint.execute(createContentBrowseActionCallback(service, treeModel, getRootNode()));
    }

    public Container getRootContainer() {
        return rootContainer;
    }

    public DefaultMutableTreeNode getRootNode() {
        return rootNode;
    }

    protected Container createRootContainer(Service service) {
        Container rootContainer = new Container();
        rootContainer.setId("0");
        rootContainer.setTitle("Content Directory on " + service.getDevice().getDisplayString());
        return rootContainer;
    }

    protected TreeWillExpandListener createContainerTreeExpandListener(ControlPoint controlPoint,
                                                                       Service service,
                                                                       DefaultTreeModel treeModel) {
        return new ContentTreeExpandListener(controlPoint, service, treeModel, this);
    }

    protected DefaultTreeCellRenderer createContainerTreeCellRenderer() {
        return new ContentTreeCellRenderer();
    }

    public ActionCallback createContentBrowseActionCallback(Service service,
                                                              DefaultTreeModel treeModel,
                                                              DefaultMutableTreeNode treeNode) {

        return new ContentBrowseActionCallback(service, treeModel, treeNode) {
            public void updateStatusUI(Status status, DefaultMutableTreeNode treeNode, DefaultTreeModel treeModel) {
                ContentTree.this.updateStatus(status, treeNode, treeModel);
            }
            public void failureUI(String failureMessage) {
                ContentTree.this.failure(failureMessage);
            }
        };
    }

    // Show some of the status messages _inside_ the tree as a special node
    public void updateStatus(ContentBrowseActionCallback.Status status, DefaultMutableTreeNode treeNode, DefaultTreeModel treeModel) {
        switch(status) {
            case LOADING:
            case NO_CONTENT:
                treeNode.removeAllChildren();
                int index = treeNode.getChildCount() <= 0 ? 0 : treeNode.getChildCount();
                treeModel.insertNodeInto(new DefaultMutableTreeNode(status.getDefaultMessage()), treeNode, index);
                treeModel.nodeStructureChanged(treeNode);
                break;
        }
    }

    public abstract void failure(String message);

}
