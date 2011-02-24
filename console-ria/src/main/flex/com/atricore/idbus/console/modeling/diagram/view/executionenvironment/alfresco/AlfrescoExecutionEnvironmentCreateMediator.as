/*
 * Atricore Console
 *
 * Copyright 2009-2010, Atricore Inc.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package com.atricore.idbus.console.modeling.diagram.view.executionenvironment.alfresco {
import com.atricore.idbus.console.main.ApplicationFacade;
import com.atricore.idbus.console.main.model.ProjectProxy;
import com.atricore.idbus.console.main.view.form.FormUtility;
import com.atricore.idbus.console.main.view.form.IocFormMediator;

import com.atricore.idbus.console.modeling.diagram.model.request.CheckFoldersRequest;
import com.atricore.idbus.console.modeling.diagram.model.request.CheckInstallFolderRequest;
import com.atricore.idbus.console.modeling.diagram.model.response.CheckFoldersResponse;
import com.atricore.idbus.console.modeling.main.controller.FolderExistsCommand;
import com.atricore.idbus.console.modeling.main.controller.FoldersExistsCommand;
import com.atricore.idbus.console.modeling.palette.PaletteMediator;
import com.atricore.idbus.console.services.dto.AlfrescoExecutionEnvironment;
import com.atricore.idbus.console.services.dto.TomcatExecutionEnvironment;

import flash.events.MouseEvent;

import mx.collections.ArrayCollection;
import mx.events.CloseEvent;

import mx.resources.IResourceManager;
import mx.resources.ResourceManager;

import org.puremvc.as3.interfaces.INotification;

public class AlfrescoExecutionEnvironmentCreateMediator extends IocFormMediator {

    private var _projectProxy:ProjectProxy;

    private static var _environmentName:String = "ALFRESCO";    

    private var _newExecutionEnvironment:AlfrescoExecutionEnvironment;

    private var resourceManager:IResourceManager = ResourceManager.getInstance();

    public function AlfrescoExecutionEnvironmentCreateMediator(name:String = null, viewComp:AlfrescoExecutionEnvironmentCreateForm = null) {
        super(name, viewComp);
    }

    public function get projectProxy():ProjectProxy {
        return _projectProxy;
    }

    public function set projectProxy(value:ProjectProxy):void {
        _projectProxy = value;
    }
    
    override public function setViewComponent(viewComponent:Object):void {
        if (getViewComponent() != null) {
            view.btnOk.removeEventListener(MouseEvent.CLICK, handleAlfrescoExecutionEnvironmentSave);
            view.btnCancel.removeEventListener(MouseEvent.CLICK, handleCancel);
        }

        super.setViewComponent(viewComponent);

        init();
    }

    private function init():void {
        view.btnOk.addEventListener(MouseEvent.CLICK, handleAlfrescoExecutionEnvironmentSave);
        view.btnCancel.addEventListener(MouseEvent.CLICK, handleCancel);
        view.selectedHost.selectedIndex = 0;
        view.selectedHost.enabled = false;
        view.focusManager.setFocus(view.executionEnvironmentName);
    }

    private function resetForm():void {
        view.executionEnvironmentName.text = "";
        view.executionEnvironmentDescription.text = "";
        view.selectedHost.selectedIndex = 0;
        view.homeDirectory.text = "";
        view.replaceConfFiles.selected = false;

        FormUtility.clearValidationErrors(_validators);
    }

    override public function bindModel():void {

        var alfrescoExecutionEnvironment:AlfrescoExecutionEnvironment = new AlfrescoExecutionEnvironment();

        alfrescoExecutionEnvironment.name = view.executionEnvironmentName.text;
        alfrescoExecutionEnvironment.description = view.executionEnvironmentDescription.text;
        alfrescoExecutionEnvironment.installUri = view.homeDirectory.text;
        alfrescoExecutionEnvironment.overwriteOriginalSetup = view.replaceConfFiles.selected;
        alfrescoExecutionEnvironment.installDemoApps = false;
        alfrescoExecutionEnvironment.platformId = "alfresco";
        alfrescoExecutionEnvironment.tomcatInstallDir = view.tomcatInstallDir.text;
        _newExecutionEnvironment = alfrescoExecutionEnvironment;
    }

    private function handleAlfrescoExecutionEnvironmentSave(event:MouseEvent):void {
        view.homeDirectory.errorString = "";
        if (validate(true)) {
            var folders:ArrayCollection = new ArrayCollection();
            folders.addItem(view.homeDirectory.text);
            folders.addItem(view.tomcatInstallDir.text);
            var cf:CheckFoldersRequest = new CheckFoldersRequest();
            cf.folders = folders;
            cf.environmentName = _environmentName;
            sendNotification(ApplicationFacade.CHECK_FOLDERS_EXISTENCE, cf);
        }        
    }

    private function save():void {
        bindModel();
        if(_projectProxy.currentIdentityAppliance.idApplianceDefinition.executionEnvironments == null){
            _projectProxy.currentIdentityAppliance.idApplianceDefinition.executionEnvironments = new ArrayCollection();
        }
        _projectProxy.currentIdentityAppliance.idApplianceDefinition.executionEnvironments.addItem(_newExecutionEnvironment);
        _projectProxy.currentIdentityApplianceElement = _newExecutionEnvironment;
        sendNotification(ApplicationFacade.DIAGRAM_ELEMENT_CREATION_COMPLETE);
        sendNotification(ApplicationFacade.UPDATE_IDENTITY_APPLIANCE);
        sendNotification(ApplicationFacade.IDENTITY_APPLIANCE_CHANGED);
        closeWindow();
    }

    private function handleCancel(event:MouseEvent):void {
        closeWindow();
    }

    private function closeWindow():void {
        resetForm();
        sendNotification(PaletteMediator.DESELECT_PALETTE_ELEMENT);
        view.parent.dispatchEvent(new CloseEvent(CloseEvent.CLOSE));
    }

    protected function get view():AlfrescoExecutionEnvironmentCreateForm
    {
        return viewComponent as AlfrescoExecutionEnvironmentCreateForm;
    }


    override public function registerValidators():void {
        _validators.push(view.nameValidator);
        _validators.push(view.homeDirValidator);
        _validators.push(view.containerDirValidator);
    }

    override public function listNotificationInterests():Array {
        return [super.listNotificationInterests(),
                FoldersExistsCommand.FOLDERS_EXISTENCE_CHECKED,
                FoldersExistsCommand.FAILURE];
    }

    override public function handleNotification(notification:INotification):void {
        switch (notification.getName()) {
            case FoldersExistsCommand.FOLDERS_EXISTENCE_CHECKED:
                var resp:CheckFoldersResponse = notification.getBody() as CheckFoldersResponse;
                if (resp.environmentName == _environmentName) {
                    if (resp.invalidFolders != null && resp.invalidFolders.length > 0) {
                        for each (var invalidFolder:String in resp.invalidFolders) {
                            if (view.homeDirectory.text == invalidFolder) {
                                view.homeDirectory.errorString = resourceManager.getString(AtricoreConsole.BUNDLE, "executionenvironment.doesntexist")
                            }
                            if (view.tomcatInstallDir.text == invalidFolder) {
                                view.tomcatInstallDir.errorString = resourceManager.getString(AtricoreConsole.BUNDLE, "executionenvironment.doesntexist");
                            }
                        }
                    } else {
                        save();
                    }
                }
                break;
        }
    }
}
}