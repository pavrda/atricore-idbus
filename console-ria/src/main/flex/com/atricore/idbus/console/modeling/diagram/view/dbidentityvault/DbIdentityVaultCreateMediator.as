/**
 * @author: sgonzalez@atriocore.com
 * @date: 12/9/13
 */
package com.atricore.idbus.console.modeling.diagram.view.dbidentityvault {
import com.atricore.idbus.console.main.ApplicationFacade;
import com.atricore.idbus.console.main.model.ProjectProxy;
import com.atricore.idbus.console.main.view.form.FormUtility;
import com.atricore.idbus.console.main.view.form.IocFormMediator;
import com.atricore.idbus.console.modeling.main.controller.JDBCDriversListCommand;
import com.atricore.idbus.console.modeling.palette.PaletteMediator;
import com.atricore.idbus.console.services.dto.DbIdentityVault;

import flash.events.Event;

import flash.events.MouseEvent;

import mx.binding.utils.BindingUtils;

import mx.collections.ArrayCollection;
import mx.events.CloseEvent;

import org.puremvc.as3.interfaces.INotification;

public class DbIdentityVaultCreateMediator extends IocFormMediator {

    private var _projectProxy:ProjectProxy;
    private var _newIdentityVault:DbIdentityVault;

    [Bindable]
    public var _jdbcDrivers:ArrayCollection;

    public function DbIdentityVaultCreateMediator(name:String = null, viewComp:DbIdentityVaultCreateForm = null) {
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
            view.btnOk.removeEventListener(MouseEvent.CLICK, handleIdentityVaultSave);
            view.btnCancel.removeEventListener(MouseEvent.CLICK, handleCancel);
            view.driver.removeEventListener(Event.CHANGE, handleDriverChange);
        }

        super.setViewComponent(viewComponent);

        init();
    }

    private function init():void {
        view.btnOk.addEventListener(MouseEvent.CLICK, handleIdentityVaultSave);
        view.btnCancel.addEventListener(MouseEvent.CLICK, handleCancel);

        BindingUtils.bindProperty(view.driver, "dataProvider", this, "_jdbcDrivers");
        view.driver.addEventListener(Event.CHANGE, handleDriverChange);
        sendNotification(ApplicationFacade.LIST_JDBC_DRIVERS);

        view.focusManager.setFocus(view.identityVaultName);
    }

    private function resetForm():void {
        view.identityVaultName.text = "";
        view.identityVaultDescription.text = "";

        FormUtility.clearValidationErrors(_validators);
    }

    override public function bindModel():void {
        var identityVault:DbIdentityVault = new DbIdentityVault();

        identityVault.name = view.identityVaultName.text;
        identityVault.description = view.identityVaultDescription.text;
        identityVault.hashAlgorithm = view.identityVaultHashAlgorithm.selectedItem.data;
        identityVault.hashEncoding = view.identityVaultHashEncoding.selectedItem.data;
        identityVault.saltLength = parseInt(view.identityVaultSaltLength.selectedItem.data);

        identityVault.username = view.username.text;
        identityVault.password = view.password.text;
        identityVault.externalDB = view.externalDB.selected;
        if (view.driver.selectedItem != null)
            identityVault.driverName = view.driver.selectedItem.className;
        identityVault.connectionUrl = view.connectionUrl.text;

        _newIdentityVault = identityVault;
    }

    private function handleIdentityVaultSave(event:MouseEvent):void {
        if (validate(true)) {
            bindModel();
            _projectProxy.currentIdentityAppliance.idApplianceDefinition.identitySources.addItem(_newIdentityVault);
            _projectProxy.currentIdentityApplianceElement = _newIdentityVault;
            sendNotification(ApplicationFacade.DIAGRAM_ELEMENT_CREATION_COMPLETE);
            sendNotification(ApplicationFacade.UPDATE_IDENTITY_APPLIANCE);
            sendNotification(ApplicationFacade.IDENTITY_APPLIANCE_CHANGED);
            closeWindow();
        }
        else {
            event.stopImmediatePropagation();
        }
    }

    private function handleDriverChange(event:Event):void {
        view.connectionUrl.text = view.driver.selectedItem.defaultUrl;
    }

    private function handleCancel(event:MouseEvent):void {
        closeWindow();
    }

    private function closeWindow():void {
        resetForm();
        sendNotification(PaletteMediator.DESELECT_PALETTE_ELEMENT);
        view.parent.dispatchEvent(new CloseEvent(CloseEvent.CLOSE));
    }

    protected function get view():DbIdentityVaultCreateForm {
        return viewComponent as DbIdentityVaultCreateForm;
    }


    override public function registerValidators():void {
        _validators.push(view.nameValidator);
        _validators.push(view.pwvPasswords);
    }


    override public function listNotificationInterests():Array {
        return [JDBCDriversListCommand.SUCCESS,
            JDBCDriversListCommand.FAILURE];
    }

    override public function handleNotification(notification:INotification):void {
        switch (notification.getName()) {
            case JDBCDriversListCommand.SUCCESS:
                _jdbcDrivers = projectProxy.jdbcDrivers;
                break;
        }
    }
}
}