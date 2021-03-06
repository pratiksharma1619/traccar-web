/*
 * Copyright 2013 Anton Tananaev (anton.tananaev@gmail.com)
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
package org.traccar.web.client.view;

import java.util.LinkedList;
import java.util.List;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Event;
import com.sencha.gxt.cell.core.client.form.CheckBoxCell;
import com.sencha.gxt.widget.core.client.TabPanel.TabPanelAppearance;
import com.sencha.gxt.theme.blue.client.tabs.BlueTabPanelBottomAppearance;
import com.sencha.gxt.widget.core.client.ListView;
import com.sencha.gxt.widget.core.client.TabPanel;
import com.sencha.gxt.widget.core.client.event.CellDoubleClickEvent;
import com.sencha.gxt.widget.core.client.event.RowMouseDownEvent;
import com.sencha.gxt.widget.core.client.form.CheckBox;
import com.sencha.gxt.widget.core.client.grid.editing.GridEditing;
import com.sencha.gxt.widget.core.client.grid.editing.GridInlineEditing;
import com.sencha.gxt.widget.core.client.toolbar.FillToolItem;
import com.sencha.gxt.widget.core.client.toolbar.SeparatorToolItem;
import org.traccar.web.client.Application;
import org.traccar.web.client.ApplicationContext;
import org.traccar.web.client.i18n.Messages;
import org.traccar.web.client.model.BaseAsyncCallback;
import org.traccar.web.client.model.DeviceProperties;
import org.traccar.web.client.model.GeoFenceProperties;
import org.traccar.web.client.state.GridStateHandler;
import org.traccar.web.shared.model.Device;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import com.sencha.gxt.core.client.Style.SelectionMode;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.widget.core.client.ContentPanel;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.grid.ColumnConfig;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.grid.Grid;
import com.sencha.gxt.widget.core.client.menu.Item;
import com.sencha.gxt.widget.core.client.menu.MenuItem;
import com.sencha.gxt.widget.core.client.selection.SelectionChangedEvent;
import org.traccar.web.shared.model.GeoFence;

public class DeviceView implements RowMouseDownEvent.RowMouseDownHandler, CellDoubleClickEvent.CellDoubleClickHandler {

    private static DeviceViewUiBinder uiBinder = GWT.create(DeviceViewUiBinder.class);

    interface DeviceViewUiBinder extends UiBinder<Widget, DeviceView> {
    }

    public interface DeviceHandler {
        public void onSelected(Device device);
        public void onAdd();
        public void onEdit(Device device);
        public void onShare(Device device);
        public void onRemove(Device device);
        public void onMouseOver(int mouseX, int mouseY, Device device);
        public void onMouseOut(int mouseX, int mouseY, Device device);
        public void doubleClicked(Device device);
    }

    public interface GeoFenceHandler {
        public void onAdd();
        public void onEdit(GeoFence geoFence);
        public void onRemove(GeoFence geoFence);
        public void onSelected(GeoFence geoFence);
        public void onShare(GeoFence geoFence);
    }

    private final DeviceHandler deviceHandler;

    private final GeoFenceHandler geoFenceHandler;

    @UiField
    ContentPanel contentPanel;

    public ContentPanel getView() {
        return contentPanel;
    }

    @UiField
    TextButton addButton;

    @UiField
    TextButton editButton;

    @UiField
    TextButton shareButton;

    @UiField
    TextButton removeButton;

    @UiField
    FillToolItem fillItem;

    @UiField
    SeparatorToolItem separatorItem;

    @UiField(provided = true)
    TabPanel objectsTabs;

    @UiField(provided = true)
    ColumnModel<Device> columnModel;

    @UiField(provided = true)
    ListStore<Device> deviceStore;

    @UiField
    Grid<Device> grid;

    @UiField(provided = true)
    ListStore<GeoFence> geoFenceStore;

    @UiField(provided = true)
    ListView<GeoFence, String> geoFenceList;

    @UiField
    TextButton settingsButton;

    @UiField
    MenuItem settingsAccount;

    @UiField
    MenuItem settingsPreferences;

    @UiField
    MenuItem settingsUsers;

    @UiField
    MenuItem settingsGlobal;

    @UiField
    MenuItem settingsNotifications;

    @UiField
    MenuItem showTrackerServerLog;

    @UiField(provided = true)
    Messages i18n = GWT.create(Messages.class);

    public DeviceView(final DeviceHandler deviceHandler,
                      final GeoFenceHandler geoFenceHandler,
                      SettingsHandler settingsHandler,
                      final ListStore<Device> deviceStore,
                      final ListStore<GeoFence> geoFenceStore) {
        this.deviceHandler = deviceHandler;
        this.geoFenceHandler = geoFenceHandler;
        this.settingsHandler = settingsHandler;
        this.deviceStore = deviceStore;
        this.geoFenceStore = geoFenceStore;

        DeviceProperties deviceProperties = GWT.create(DeviceProperties.class);

        List<ColumnConfig<Device, ?>> columnConfigList = new LinkedList<ColumnConfig<Device, ?>>();

        ColumnConfig<Device, String> colName = new ColumnConfig<Device, String>(deviceProperties.name(), 0, i18n.name());
        colName.setCell(new AbstractCell<String>(BrowserEvents.MOUSEOVER, BrowserEvents.MOUSEOUT) {
            @Override
            public void render(Context context, String value, SafeHtmlBuilder sb) {
                if (value == null) return;
                sb.appendEscaped(value);
            }

            @Override
            public void onBrowserEvent(Context context, Element parent, String value, NativeEvent event, ValueUpdater<String> valueUpdater) {
                if (event.getType().equals(BrowserEvents.MOUSEOVER) || event.getType().equals(BrowserEvents.MOUSEOUT)) {
                    Element target = Element.as(event.getEventTarget());
                    int rowIndex = grid.getView().findRowIndex(target);
                    if (rowIndex != -1) {
                        if (event.getType().equals(BrowserEvents.MOUSEOVER)) {
                            deviceHandler.onMouseOver(event.getClientX(), event.getClientY(), deviceStore.get(rowIndex));
                        } else {
                            deviceHandler.onMouseOut(event.getClientX(), event.getClientY(), deviceStore.get(rowIndex));
                        }
                    }
                } else {
                    super.onBrowserEvent(context, parent, value, event, valueUpdater);
                }
            }
        });
        columnConfigList.add(colName);

        ColumnConfig<Device, Boolean> colFollow = new ColumnConfig<Device, Boolean>(deviceProperties.follow(), 50, i18n.follow());
        colFollow.setCell(new CheckBoxCell());
        colFollow.setFixed(true);
        colFollow.setResizable(false);
        columnConfigList.add(colFollow);

        ColumnConfig<Device, Boolean> colRecordTrace = new ColumnConfig<Device, Boolean>(deviceProperties.recordTrace(), 60, i18n.recordTrace());
        colRecordTrace.setCell(new CheckBoxCell());
        colRecordTrace.setFixed(true);
        colRecordTrace.setResizable(false);
        columnConfigList.add(colRecordTrace);

        columnModel = new ColumnModel<Device>(columnConfigList);

        // geo-fences
        GeoFenceProperties geoFenceProperties = GWT.create(GeoFenceProperties.class);

        geoFenceList = new ListView<GeoFence, String>(geoFenceStore, geoFenceProperties.name()) {
            @Override
            protected void onMouseDown(Event e) {
                int index = indexOf(e.getEventTarget().<Element>cast());
                if (index != -1) {
                    geoFenceHandler.onSelected(geoFenceList.getStore().get(index));
                }
                super.onMouseDown(e);
            }
        };
        geoFenceList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        geoFenceList.getSelectionModel().addSelectionChangedHandler(geoFenceSelectionHandler);

        // tab panel
        objectsTabs = new TabPanel(GWT.<TabPanelAppearance>create(BlueTabPanelBottomAppearance.class));

        uiBinder.createAndBindUi(this);

        grid.getSelectionModel().addSelectionChangedHandler(deviceSelectionHandler);
        grid.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        grid.addRowMouseDownHandler(this);
        grid.addCellDoubleClickHandler(this);

        grid.getView().setAutoFill(true);
        grid.getView().setForceFit(true);

        new GridStateHandler<Device>(grid).loadState();

        GridEditing<Device> editing = new GridInlineEditing<Device>(grid);
        grid.getView().setShowDirtyCells(false);
        editing.addEditor(colFollow, new CheckBox());
        editing.addEditor(colRecordTrace, new CheckBox());

        boolean readOnly = ApplicationContext.getInstance().getUser().getReadOnly();
        boolean admin = ApplicationContext.getInstance().getUser().getAdmin();
        boolean manager = ApplicationContext.getInstance().getUser().getManager();
        boolean allowDeviceManagement = !ApplicationContext.getInstance().getApplicationSettings().isDisallowDeviceManagementByUsers();

        settingsButton.setVisible(admin || !readOnly);
        settingsAccount.setVisible(!readOnly);
        settingsPreferences.setVisible(!readOnly);

        settingsGlobal.setVisible(!readOnly && admin);
        showTrackerServerLog.setVisible(admin);
        settingsUsers.setVisible(!readOnly && (admin || manager));
        settingsNotifications.setVisible(!readOnly && (admin || manager));
        shareButton.setVisible(!readOnly && (admin || manager));

        addButton.setVisible(!readOnly && (allowDeviceManagement || admin || manager));
        editButton.setVisible(!readOnly && (allowDeviceManagement || admin || manager));
        removeButton.setVisible(!readOnly && (allowDeviceManagement || admin || manager));
        fillItem.setVisible(!readOnly && (allowDeviceManagement || admin || manager));
        separatorItem.setVisible(!readOnly && (allowDeviceManagement || admin || manager));
    }

    final SelectionChangedEvent.SelectionChangedHandler<Device> deviceSelectionHandler = new SelectionChangedEvent.SelectionChangedHandler<Device>() {
        @Override
        public void onSelectionChanged(SelectionChangedEvent<Device> event) {
            editButton.setEnabled(!event.getSelection().isEmpty());
            shareButton.setEnabled(!event.getSelection().isEmpty());
            removeButton.setEnabled(!event.getSelection().isEmpty());
        }
    };

    final SelectionChangedEvent.SelectionChangedHandler<GeoFence> geoFenceSelectionHandler = new SelectionChangedEvent.SelectionChangedHandler<GeoFence>() {
        @Override
        public void onSelectionChanged(SelectionChangedEvent<GeoFence> event) {
            editButton.setEnabled(!event.getSelection().isEmpty());
            shareButton.setEnabled(!event.getSelection().isEmpty());
            removeButton.setEnabled(!event.getSelection().isEmpty());

            geoFenceHandler.onSelected(event.getSelection().isEmpty() ? null : event.getSelection().get(0));
        }
    };

    @Override
    public void onRowMouseDown(RowMouseDownEvent event) {
        deviceHandler.onSelected(grid.getSelectionModel().getSelectedItem());
    }

    @Override
    public void onCellClick(CellDoubleClickEvent cellDoubleClickEvent) {
        deviceHandler.doubleClicked(grid.getSelectionModel().getSelectedItem());
    }

    @UiHandler("addButton")
    public void onAddClicked(SelectEvent event) {
        if (objectsTabs.getActiveWidget() == geoFenceList) {
            geoFenceHandler.onAdd();
        } else {
            deviceHandler.onAdd();
        }
    }

    @UiHandler("editButton")
    public void onEditClicked(SelectEvent event) {
        if (objectsTabs.getActiveWidget() == geoFenceList) {
            geoFenceHandler.onEdit(geoFenceList.getSelectionModel().getSelectedItem());
        } else {
            deviceHandler.onEdit(grid.getSelectionModel().getSelectedItem());
        }
    }

    @UiHandler("shareButton")
    public void onShareClicked(SelectEvent event) {
        if (objectsTabs.getActiveWidget() == geoFenceList) {
            geoFenceHandler.onShare(geoFenceList.getSelectionModel().getSelectedItem());
        } else {
            deviceHandler.onShare(grid.getSelectionModel().getSelectedItem());
        }
    }

    @UiHandler("removeButton")
    public void onRemoveClicked(SelectEvent event) {
        if (objectsTabs.getActiveWidget() == geoFenceList) {
            geoFenceHandler.onRemove(geoFenceList.getSelectionModel().getSelectedItem());
        } else {
            deviceHandler.onRemove(grid.getSelectionModel().getSelectedItem());
        }
    }

    @UiHandler("logoutButton")
    public void onLogoutClicked(SelectEvent event) {
        Application.getDataService().logout(new BaseAsyncCallback<Boolean>(i18n) {
            @Override
            public void onSuccess(Boolean result) {
                Window.Location.reload();
            }
        });
    }

    public void selectDevice(Device device) {
        grid.getSelectionModel().select(deviceStore.findModel(device), false);
    }

    public interface SettingsHandler {
        public void onAccountSelected();
        public void onPreferencesSelected();
        public void onUsersSelected();
        public void onApplicationSelected();
        public void onNotificationsSelected();
    }

    private SettingsHandler settingsHandler;

    @UiHandler("settingsAccount")
    public void onSettingsAccountSelected(SelectionEvent<Item> event) {
        settingsHandler.onAccountSelected();
    }

    @UiHandler("settingsPreferences")
    public void onSettingsPreferencesSelected(SelectionEvent<Item> event) {
        settingsHandler.onPreferencesSelected();
    }

    @UiHandler("settingsUsers")
    public void onSettingsUsersSelected(SelectionEvent<Item> event) {
        settingsHandler.onUsersSelected();
    }

    @UiHandler("settingsGlobal")
    public void onSettingsGlobalSelected(SelectionEvent<Item> event) {
        settingsHandler.onApplicationSelected();
    }

    @UiHandler("settingsNotifications")
    public void onSettingsNotificationsSelected(SelectionEvent<Item> event) {
        settingsHandler.onNotificationsSelected();
    }

    @UiHandler("showTrackerServerLog")
    public void onShowTrackerServerLog(SelectionEvent<Item> event) {
        new TrackerServerLogViewDialog().show();
    }

    @UiHandler("objectsTabs")
    public void onTabSelected(SelectionEvent<Widget> event) {
        if (event.getSelectedItem() == geoFenceList) {
            grid.getSelectionModel().deselectAll();
        } else {
            geoFenceList.getSelectionModel().deselectAll();
        }
    }
}
