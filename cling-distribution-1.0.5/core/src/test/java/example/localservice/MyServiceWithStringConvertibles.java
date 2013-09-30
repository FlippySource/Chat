package example.localservice;

import org.teleal.cling.binding.annotations.*;
import java.net.URI;
import java.net.URL;
import java.util.List;
import org.teleal.cling.model.types.csv.CSV;        // DOC:INC1
import org.teleal.cling.model.types.csv.CSVInteger;

@UpnpService(
        serviceId = @UpnpServiceId("MyService"),
        serviceType = @UpnpServiceType(namespace = "mydomain", value = "MyService"),
        stringConvertibleTypes = MyStringConvertible.class
)
public class MyServiceWithStringConvertibles {

    @UpnpStateVariable
    private URL myURL;

    @UpnpStateVariable
    private URI myURI;

    @UpnpStateVariable(datatype = "string")
    private List<Integer> myNumbers;

    @UpnpStateVariable
    private MyStringConvertible myStringConvertible;

    @UpnpAction(out = @UpnpOutputArgument(name = "Out"))
    public URL getMyURL() {
        return myURL;
    }

    @UpnpAction
    public void setMyURL(@UpnpInputArgument(name = "In") URL myURL) {
        this.myURL = myURL;
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "Out"))
    public URI getMyURI() {
        return myURI;
    }

    @UpnpAction
    public void setMyURI(@UpnpInputArgument(name = "In") URI myURI) {
        this.myURI = myURI;
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "Out"))
    public CSV<Integer> getMyNumbers() {
        CSVInteger wrapper = new CSVInteger();
        if (myNumbers != null)
            wrapper.addAll(myNumbers);
        return wrapper;
    }

    @UpnpAction
    public void setMyNumbers(
            @UpnpInputArgument(name = "In")
            CSVInteger myNumbers
    ) {
        this.myNumbers = myNumbers;
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "Out"))
    public MyStringConvertible getMyStringConvertible() {
        return myStringConvertible;
    }

    @UpnpAction
    public void setMyStringConvertible(
            @UpnpInputArgument(name = "In")
            MyStringConvertible myStringConvertible
    ) {
        this.myStringConvertible = myStringConvertible;
    }
} // DOC:INC1
