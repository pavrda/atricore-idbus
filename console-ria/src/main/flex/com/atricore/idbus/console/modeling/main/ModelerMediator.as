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

package com.atricore.idbus.console.modeling.main {
import com.atricore.idbus.console.main.ApplicationFacade;
import com.atricore.idbus.console.main.model.ProjectProxy;
import com.atricore.idbus.console.main.view.progress.ProcessingMediator;
import com.atricore.idbus.console.modeling.browser.BrowserMediator;
import com.atricore.idbus.console.modeling.diagram.DiagramMediator;
import com.atricore.idbus.console.modeling.diagram.model.request.RemoveIdentityApplianceElementRequest;
import com.atricore.idbus.console.modeling.diagram.model.request.RemoveIdentityProviderElementRequest;
import com.atricore.idbus.console.modeling.diagram.model.request.RemoveIdentityVaultElementRequest;
import com.atricore.idbus.console.modeling.diagram.model.request.RemoveIdpChannelElementRequest;
import com.atricore.idbus.console.modeling.diagram.model.request.RemoveServiceProviderElementRequest;
import com.atricore.idbus.console.modeling.diagram.model.request.RemoveSpChannelElementRequest;
import com.atricore.idbus.console.modeling.main.controller.IdentityApplianceListLoadCommand;
import com.atricore.idbus.console.modeling.main.controller.IdentityApplianceUpdateCommand;
import com.atricore.idbus.console.modeling.main.controller.LookupIdentityApplianceByIdCommand;
import com.atricore.idbus.console.modeling.main.view.*;
import com.atricore.idbus.console.modeling.main.view.appliance.IdentityApplianceMediator;
import com.atricore.idbus.console.modeling.main.view.build.BuildApplianceMediator;
import com.atricore.idbus.console.modeling.main.view.deploy.DeployApplianceMediator;
import com.atricore.idbus.console.modeling.main.view.sso.SimpleSSOWizardViewMediator;
import com.atricore.idbus.console.modeling.palette.PaletteMediator;
import com.atricore.idbus.console.modeling.propertysheet.PropertySheetMediator;
import com.atricore.idbus.console.services.dto.IdentityApplianceDTO;

import flash.events.MouseEvent;

import mx.controls.ButtonBar;
import mx.controls.buttonBarClasses.ButtonBarButton;
import mx.events.ItemClickEvent;

import org.puremvc.as3.interfaces.INotification;
import org.puremvc.as3.patterns.mediator.Mediator;
import org.springextensions.actionscript.puremvc.patterns.mediator.IocMediator;

public class ModelerMediator extends IocMediator {

    public static const BUNDLE:String = "console";

    public static const ORIENTATION_MENU_ITEM_INDEX:int = 3;

    //private static const MODEL_ACTION_BAR_NEW_BUTTON_IDX:int = 0;

    private static const MODEL_ACTION_BAR_BUILD_BUTTON_IDX:int = 0;

    private static const MODEL_ACTION_BAR_DEPLOY_BUTTON_IDX:int = 1;


    private var _modelActionToolBar:ButtonBar;

    private var _identityAppliance:IdentityApplianceDTO;

    private var _emptyNotationModel:XML;

    private var _modelerPopUpManager:ModelerPopUpManager;

    public static const NAME:String = "ModelMediator";

    [Bindable]
    public var _applianceList:Array;


    public function ModelerMediator(p_mediatorName:String = null, p_viewComponent:Object = null) {
      super(p_mediatorName, p_viewComponent);
    }

    /*
    public function ModelerMediator(viewComp:ModelerView) {
        super(NAME, viewComp);

        // register mediators for child components
        facade.registerMediator(new BrowserMediator(viewComp.browser));
        facade.registerMediator(new DiagramMediator(viewComp.diagram));
        facade.registerMediator(new PaletteMediator(viewComp.palette));
        facade.registerMediator(new PropertySheetMediator(viewComp.propertysheet));
    }
    */


    override public function setViewComponent(p_viewComponent:Object):void {
      if (getViewComponent() != null) {
          view.btnNew.removeEventListener(MouseEvent.CLICK, handleNewClick);
          view.btnOpen.removeEventListener(MouseEvent.CLICK, handleOpenClick);
          view.btnSave.removeEventListener(MouseEvent.CLICK, handleSaveClick);
          _modelActionToolBar.removeEventListener(ItemClickEvent.ITEM_CLICK, handleModelActionToolBarClick);
      }

      super.setViewComponent(p_viewComponent);

      init();
    }

    public function init():void {
        _modelActionToolBar = view.modelActionToolBar;

        //(_modelActionToolBar.getChildAt(MODEL_ACTION_BAR_NEW_BUTTON_IDX) as ButtonBarButton).enabled = true;
        (_modelActionToolBar.getChildAt(MODEL_ACTION_BAR_BUILD_BUTTON_IDX) as ButtonBarButton).enabled = false;
        (_modelActionToolBar.getChildAt(MODEL_ACTION_BAR_DEPLOY_BUTTON_IDX) as ButtonBarButton).enabled = false;

        view.btnNew.addEventListener(MouseEvent.CLICK, handleNewClick);
        view.btnOpen.addEventListener(MouseEvent.CLICK, handleOpenClick);
        view.btnSave.addEventListener(MouseEvent.CLICK, handleSaveClick);
        _modelActionToolBar.addEventListener(ItemClickEvent.ITEM_CLICK, handleModelActionToolBarClick);

        view.appliances.labelFunction = applianceListLabelFunc;
        view.btnSave.enabled = false;

        _modelerPopUpManager = new ModelerPopUpManager(facade, view);

        sendNotification(ApplicationFacade.NOTE_IDENTITY_APPLIANCE_LIST_LOAD);
    }


    private function handleNewClick(event:MouseEvent):void {
        trace("New Button Click: " + event);
        if (view.applianceStyle.selectedItem.data == "Advanced") {
            sendNotification(IdentityApplianceMediator.CREATE);
        } else if (view.applianceStyle.selectedItem.data == "SimpleSSO") {
            sendNotification(SimpleSSOWizardViewMediator.RUN);
        }
    }

    private function handleOpenClick(event:MouseEvent):void {
        trace("Open Button Click: " + event);
        if (view.appliances.selectedItem != null) {
            var applianceId:String = (view.appliances.selectedItem as IdentityApplianceDTO).id.toString();
            sendNotification(ApplicationFacade.NOTE_LOOKUP_IDENTITY_APPLIANCE_BY_ID, applianceId);
        }
    }

    private function handleSaveClick(event:MouseEvent):void {
        trace("Save Button Click: " + event);
        sendNotification(ApplicationFacade.NOTE_EDIT_IDENTITY_APPLIANCE);
    }

    private function handleModelActionToolBarClick(event:ItemClickEvent):void {
        if (event.index == 0) {
            trace("Build Button Click: " + event);
            sendNotification(BuildApplianceMediator.RUN);
        } else if (event.index == 1) {
            trace("Deploy Button Click: " + event);
            sendNotification(DeployApplianceMediator.RUN);
        }
    }

    override public function listNotificationInterests():Array {
        return [ApplicationFacade.NOTE_UPDATE_IDENTITY_APPLIANCE,
            ApplicationFacade.NOTE_REMOVE_IDENTITY_APPLIANCE_ELEMENT,
            ApplicationFacade.NOTE_CREATE_IDENTITY_PROVIDER_ELEMENT,
            ApplicationFacade.NOTE_REMOVE_IDENTITY_PROVIDER_ELEMENT,
            ApplicationFacade.NOTE_CREATE_SERVICE_PROVIDER_ELEMENT,
            ApplicationFacade.NOTE_REMOVE_SERVICE_PROVIDER_ELEMENT,
            ApplicationFacade.NOTE_CREATE_IDP_CHANNEL_ELEMENT,
            ApplicationFacade.NOTE_REMOVE_IDP_CHANNEL_ELEMENT,
            ApplicationFacade.NOTE_CREATE_SP_CHANNEL_ELEMENT,
            ApplicationFacade.NOTE_REMOVE_SP_CHANNEL_ELEMENT,
            ApplicationFacade.NOTE_CREATE_DB_IDENTITY_VAULT_ELEMENT,
            ApplicationFacade.NOTE_REMOVE_DB_IDENTITY_VAULT_ELEMENT,
            ApplicationFacade.NOTE_MANAGE_CERTIFICATE,
            ApplicationFacade.NOTE_SHOW_UPLOAD_PROGRESS,
            ApplicationFacade.NOTE_IDENTITY_APPLIANCE_CHANGED,
            ProcessingMediator.START,
            BuildApplianceMediator.RUN,
            DeployApplianceMediator.RUN,
            LookupIdentityApplianceByIdCommand.SUCCESS,
            LookupIdentityApplianceByIdCommand.FAILURE,
            IdentityApplianceListLoadCommand.SUCCESS,
            IdentityApplianceListLoadCommand.FAILURE,
            IdentityApplianceUpdateCommand.SUCCESS,
            IdentityApplianceUpdateCommand.FAILURE];
    }

    override public function handleNotification(notification:INotification):void {
        switch (notification.getName()) {
            case ApplicationFacade.NOTE_UPDATE_IDENTITY_APPLIANCE:
                updateIdentityAppliance();
                enableIdentityApplianceActionButtons();
                break;
            case ApplicationFacade.NOTE_REMOVE_IDENTITY_APPLIANCE_ELEMENT:
                var ria:RemoveIdentityApplianceElementRequest  = RemoveIdentityApplianceElementRequest(notification.getBody());
                // TODO: Perform UI handling for confirming removal action
                sendNotification(ApplicationFacade.NOTE_IDENTITY_APPLIANCE_REMOVE, ria.identityAppliance);
                break;
                break;
            case ApplicationFacade.NOTE_CREATE_IDENTITY_PROVIDER_ELEMENT:
                _modelerPopUpManager.showCreateIdentityProviderWindow(notification);
                break;
            case ApplicationFacade.NOTE_REMOVE_IDENTITY_PROVIDER_ELEMENT:
                var rip:RemoveIdentityProviderElementRequest  = RemoveIdentityProviderElementRequest(notification.getBody());
                // TODO: Perform UI handling for confirming removal action
                sendNotification(ApplicationFacade.NOTE_IDENTITY_PROVIDER_REMOVE, rip.identityProvider);
                break;
            case ApplicationFacade.NOTE_CREATE_SERVICE_PROVIDER_ELEMENT:
                _modelerPopUpManager.showCreateServiceProviderWindow(notification);
                break;
            case ApplicationFacade.NOTE_REMOVE_SERVICE_PROVIDER_ELEMENT:
                var rsp:RemoveServiceProviderElementRequest  = RemoveServiceProviderElementRequest(notification.getBody());
//                 TODO: Perform UI handling for confirming removal action
                sendNotification(ApplicationFacade.NOTE_SERVICE_PROVIDER_REMOVE, rsp.serviceProvider);
                break;
            case ApplicationFacade.NOTE_CREATE_IDP_CHANNEL_ELEMENT:
                _modelerPopUpManager.showCreateIdpChannelWindow(notification);
                break;
            case ApplicationFacade.NOTE_REMOVE_IDP_CHANNEL_ELEMENT:
                var ridpc:RemoveIdpChannelElementRequest  = RemoveIdpChannelElementRequest(notification.getBody());
//                 TODO: Perform UI handling for confirming removal action
                sendNotification(ApplicationFacade.NOTE_IDP_CHANNEL_REMOVE, ridpc.idpChannel);
                break;
            case ApplicationFacade.NOTE_CREATE_SP_CHANNEL_ELEMENT:
                _modelerPopUpManager.showCreateSpChannelWindow(notification);
                break;
            case ApplicationFacade.NOTE_REMOVE_SP_CHANNEL_ELEMENT:
                var rspc:RemoveSpChannelElementRequest  = RemoveSpChannelElementRequest(notification.getBody());
//                 TODO: Perform UI handling for confirming removal action
                sendNotification(ApplicationFacade.NOTE_SP_CHANNEL_REMOVE, rspc.spChannel);
                break;

            case ApplicationFacade.NOTE_CREATE_DB_IDENTITY_VAULT_ELEMENT:
                _modelerPopUpManager.showCreateDbIdentityVaultWindow(notification);
                break;
            case ApplicationFacade.NOTE_REMOVE_DB_IDENTITY_VAULT_ELEMENT:
                var rdbiv:RemoveIdentityVaultElementRequest  = RemoveIdentityVaultElementRequest(notification.getBody());
//                 TODO: Perform UI handling for confirming removal action
                sendNotification(ApplicationFacade.NOTE_DB_IDENTITY_VAULT_REMOVE, rdbiv.identityVault);
                break;
            case ApplicationFacade.NOTE_MANAGE_CERTIFICATE:
                _modelerPopUpManager.showManageCertificateWindow(notification);
                break;
            case ApplicationFacade.NOTE_SHOW_UPLOAD_PROGRESS:
                _modelerPopUpManager.showUploadProgressWindow(notification);
                break;
            case ApplicationFacade.NOTE_IDENTITY_APPLIANCE_CHANGED:
                view.btnSave.enabled = true;
                break;
            case ProcessingMediator.START:
                _modelerPopUpManager.showProcessingWindow(notification);
                break;
            case BuildApplianceMediator.RUN:
                _modelerPopUpManager.showBuildIdentityApplianceWindow(notification);
                break;
            case DeployApplianceMediator.RUN:
                _modelerPopUpManager.showDeployIdentityApplianceWindow(notification);
                break;
            case LookupIdentityApplianceByIdCommand.SUCCESS:
                view.btnSave.enabled = false;
                sendNotification(ApplicationFacade.NOTE_DISPLAY_APPLIANCE_MODELER);
                sendNotification(ApplicationFacade.NOTE_UPDATE_IDENTITY_APPLIANCE);
                sendNotification(ApplicationFacade.NOTE_SHOW_SUCCESS_MSG,
                    "Appliance successfully opened.");
                break;
            case LookupIdentityApplianceByIdCommand.FAILURE:
                sendNotification(ApplicationFacade.NOTE_SHOW_ERROR_MSG,
                    "There was an error opening appliance.");
                break;
            case IdentityApplianceListLoadCommand.SUCCESS:
                var proxy:ProjectProxy = facade.retrieveProxy(ProjectProxy.NAME) as ProjectProxy;
                view.appliances.dataProvider = proxy.identityApplianceList;
                break;
            case IdentityApplianceListLoadCommand.FAILURE:
                sendNotification(ApplicationFacade.NOTE_SHOW_ERROR_MSG,
                    "There was an error retrieving list of appliances.");
                break;
            case IdentityApplianceUpdateCommand.SUCCESS:
                view.btnSave.enabled = false;
                sendNotification(ApplicationFacade.NOTE_DISPLAY_APPLIANCE_MODELER);
                sendNotification(ApplicationFacade.NOTE_UPDATE_IDENTITY_APPLIANCE);
                sendNotification(ApplicationFacade.NOTE_SHOW_SUCCESS_MSG,
                    "Appliance successfully updated.");
                break;
            case IdentityApplianceUpdateCommand.FAILURE:
                sendNotification(ApplicationFacade.NOTE_SHOW_ERROR_MSG,
                    "There was an error updating appliance.");
                break;
        }

    }

    private function updateIdentityAppliance():void {

        var proxy:ProjectProxy = facade.retrieveProxy(ProjectProxy.NAME) as ProjectProxy;
        _identityAppliance = proxy.currentIdentityAppliance;
    }

    private function enableIdentityApplianceActionButtons():void {
        (_modelActionToolBar.getChildAt(MODEL_ACTION_BAR_BUILD_BUTTON_IDX) as ButtonBarButton).enabled = true;
        (_modelActionToolBar.getChildAt(MODEL_ACTION_BAR_DEPLOY_BUTTON_IDX) as ButtonBarButton).enabled = true;

        // TODO: associate behavior to build and deploy buttons

    }

    private function applianceListLabelFunc(item:Object):String {
        return (item as IdentityApplianceDTO).idApplianceDefinition.name;
    }

    protected function get view():ModelerView
    {
        return viewComponent as ModelerView;
    }
}
}