package example.localservice;

import org.teleal.cling.binding.annotations.*;

/**
 * Getting an output value from another method
 * <p>
 * In the following example, the UPnP action has an output argument but the
 * mapped method is void and does not return any value:
 * </p>
 * <a class="citation" href="javacode://this" style="include:INC1"/>
 * <p>
 * By providing a <code>getterName</code> in the annotation you can instruct
 * Cling to call this getter method when the action method completes, taking
 * the getter method's return value as the output argument value. If there
 * are several output arguments you can map each to a different getter method.
 * </p>
 */
@UpnpService(
        serviceId = @UpnpServiceId("SwitchPower"),
        serviceType = @UpnpServiceType(value = "SwitchPower", version = 1)
)
public class SwitchPowerExtraGetter {

    @UpnpStateVariable(defaultValue = "0", sendEvents = false)
    private boolean target = false;

    @UpnpStateVariable(defaultValue = "0")
    private boolean status = false;

    @UpnpAction
    public void setTarget(@UpnpInputArgument(name = "NewTargetValue")
                          boolean newTargetValue) {
        target = newTargetValue;
        status = newTargetValue;
        System.out.println("Switch is: " + status);
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "RetTargetValue"))
    public boolean getTarget() {
        return target;
    }

    public boolean getStatus() {                    // DOC:INC1
        return status;
    }

    @UpnpAction(
            name = "GetStatus",
            out = @UpnpOutputArgument(
                    name = "ResultStatus",
                    getterName = "getStatus"
            )
    )
    public void retrieveStatus() {
        // NOOP in this example
    }                                               // DOC:INC1

}
