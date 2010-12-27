package com.atricore.idbus.console.liveservices.liveupdate.main.engine;

import com.atricore.idbus.console.liveservices.liveupdate.main.LiveUpdateException;

/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public interface InstallOperation {

    String getStepName();

    OperationStatus preInstall(UpdateContext ctx) throws LiveUpdateException;

    OperationStatus postInstall(UpdateContext ctx) throws LiveUpdateException;

    String getName();

}
