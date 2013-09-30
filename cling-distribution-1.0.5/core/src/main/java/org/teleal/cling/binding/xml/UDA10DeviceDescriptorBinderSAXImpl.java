/*
 * Copyright (C) 2011 Teleal GmbH, Switzerland
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

package org.teleal.cling.binding.xml;

import org.teleal.cling.binding.staging.MutableDevice;
import org.teleal.cling.binding.staging.MutableIcon;
import org.teleal.cling.binding.staging.MutableService;
import org.teleal.cling.binding.staging.MutableUDAVersion;
import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.types.DLNACaps;
import org.teleal.cling.model.types.DLNADoc;
import org.teleal.cling.model.types.InvalidValueException;
import org.teleal.cling.model.types.ServiceId;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.UDN;
import org.teleal.common.xml.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.teleal.cling.binding.xml.Descriptor.Device.ELEMENT;

/**
 * A JAXP SAX parser implementation, which is actually slower than the DOM implementation (on desktop and on Android)!
 *
 * @author Christian Bauer
 */
public class UDA10DeviceDescriptorBinderSAXImpl extends UDA10DeviceDescriptorBinderImpl {

    private static Logger log = Logger.getLogger(DeviceDescriptorBinder.class.getName());

    @Override
    public <D extends Device> D describe(D undescribedDevice, String descriptorXml) throws DescriptorBindingException, ValidationException {

        if (descriptorXml == null || descriptorXml.length() == 0) {
            throw new DescriptorBindingException("Null or empty descriptor");
        }

        try {
            log.fine("Populating device from XML descriptor: " + undescribedDevice);

            // Read the XML into a mutable descriptor graph

            SAXParser parser = new SAXParser();

            MutableDevice descriptor = new MutableDevice();
            new RootHandler(descriptor, parser);

            parser.parse(
                    new InputSource(
                            // TODO: UPNP VIOLATION: Virgin Media Superhub sends trailing spaces/newlines after last XML element, need to trim()
                            new StringReader(descriptorXml.trim())
                    )
            );

            // Build the immutable descriptor graph
            return (D) descriptor.build(undescribedDevice);

        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DescriptorBindingException("Could not parse device descriptor: " + ex.toString(), ex);
        }
    }

    protected static class RootHandler extends DeviceDescriptorHandler<MutableDevice> {

        public RootHandler(MutableDevice instance, SAXParser parser) {
            super(instance, parser);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {

            if (element.equals(SpecVersionHandler.EL)) {
                MutableUDAVersion udaVersion = new MutableUDAVersion();
                getInstance().udaVersion = udaVersion;
                new SpecVersionHandler(udaVersion, this);
            }

            if (element.equals(DeviceHandler.EL)) {
                new DeviceHandler(getInstance(), this);
            }

        }

        @Override
        public void endElement(ELEMENT element) throws SAXException {
            switch (element) {
                case URLBase:
                    try {
                        // We hope it's  RFC 2396 and RFC 2732 compliant
                        getInstance().baseURL = new URL(getCharacters());
                    } catch (Exception ex) {
                        throw new SAXException("Invalid URLBase: " + ex.toString());
                    }
                    break;
            }
        }
    }

    protected static class SpecVersionHandler extends DeviceDescriptorHandler<MutableUDAVersion> {

        public static final ELEMENT EL = ELEMENT.specVersion;

        public SpecVersionHandler(MutableUDAVersion instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void endElement(ELEMENT element) throws SAXException {
            switch (element) {
                case major:
                    getInstance().major = Integer.valueOf(getCharacters());
                    break;
                case minor:
                    getInstance().minor = Integer.valueOf(getCharacters());
                    break;
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class DeviceHandler extends DeviceDescriptorHandler<MutableDevice> {

        public static final ELEMENT EL = ELEMENT.device;

        public DeviceHandler(MutableDevice instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {

            if (element.equals(IconListHandler.EL)) {
                List<MutableIcon> icons = new ArrayList();
                getInstance().icons = icons;
                new IconListHandler(icons, this);
            }

            if (element.equals(ServiceListHandler.EL)) {
                List<MutableService> services = new ArrayList();
                getInstance().services = services;
                new ServiceListHandler(services, this);
            }

            if (element.equals(DeviceListHandler.EL)) {
                List<MutableDevice> devices = new ArrayList();
                getInstance().embeddedDevices = devices;
                new DeviceListHandler(devices, this);
            }
        }

        @Override
        public void endElement(ELEMENT element) throws SAXException {
            switch (element) {
                case deviceType:
                    getInstance().deviceType = getCharacters();
                    break;
                case friendlyName:
                    getInstance().friendlyName = getCharacters();
                    break;
                case manufacturer:
                    getInstance().manufacturer = getCharacters();
                    break;
                case manufacturerURL:
                    getInstance().manufacturerURI = parseURI(getCharacters());
                    break;
                case modelDescription:
                    getInstance().modelDescription = getCharacters();
                    break;
                case modelName:
                    getInstance().modelName = getCharacters();
                    break;
                case modelNumber:
                    getInstance().modelNumber = getCharacters();
                    break;
                case modelURL:
                    getInstance().modelURI = parseURI(getCharacters());
                    break;
                case presentationURL:
                    getInstance().presentationURI = parseURI(getCharacters());
                    break;
                case UPC:
                    getInstance().upc = getCharacters();
                    break;
                case serialNumber:
                    getInstance().serialNumber = getCharacters();
                    break;
                case UDN:
                    getInstance().udn = UDN.valueOf(getCharacters());
                    break;
                case X_DLNADOC:
                    String txt = getCharacters();
                    try {
                        getInstance().dlnaDocs.add(DLNADoc.valueOf(txt));
                    } catch (InvalidValueException ex) {
                        log.info("Invalid X_DLNADOC value, ignoring value: " + txt);
                    }
                    break;
                case X_DLNACAP:
                    getInstance().dlnaCaps = DLNACaps.valueOf(getCharacters());
                    break;
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class IconListHandler extends DeviceDescriptorHandler<List<MutableIcon>> {

        public static final ELEMENT EL = ELEMENT.iconList;

        public IconListHandler(List<MutableIcon> instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
            if (element.equals(IconHandler.EL)) {
                MutableIcon icon = new MutableIcon();
                getInstance().add(icon);
                new IconHandler(icon, this);
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class IconHandler extends DeviceDescriptorHandler<MutableIcon> {

        public static final ELEMENT EL = ELEMENT.icon;

        public IconHandler(MutableIcon instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void endElement(ELEMENT element) throws SAXException {
            switch (element) {
                case width:
                    getInstance().width = Integer.valueOf(getCharacters());
                    break;
                case height:
                    getInstance().height = Integer.valueOf(getCharacters());
                    break;
                case depth:
                    getInstance().depth = Integer.valueOf(getCharacters());
                    break;
                case url:
                    getInstance().uri = parseURI(getCharacters());
                    break;
                case mimetype:
                    getInstance().mimeType = getCharacters();
                    break;
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class ServiceListHandler extends DeviceDescriptorHandler<List<MutableService>> {

        public static final ELEMENT EL = ELEMENT.serviceList;

        public ServiceListHandler(List<MutableService> instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
            if (element.equals(ServiceHandler.EL)) {
                MutableService service = new MutableService();
                getInstance().add(service);
                new ServiceHandler(service, this);
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class ServiceHandler extends DeviceDescriptorHandler<MutableService> {

        public static final ELEMENT EL = ELEMENT.service;

        public ServiceHandler(MutableService instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void endElement(ELEMENT element) throws SAXException {
            switch (element) {
                case serviceType:
                    getInstance().serviceType = ServiceType.valueOf(getCharacters());
                    break;
                case serviceId:
                    getInstance().serviceId = ServiceId.valueOf(getCharacters());
                    break;
                case SCPDURL:
                    getInstance().descriptorURI = parseURI(getCharacters());
                    break;
                case controlURL:
                    getInstance().controlURI = parseURI(getCharacters());
                    break;
                case eventSubURL:
                    getInstance().eventSubscriptionURI = parseURI(getCharacters());
                    break;
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class DeviceListHandler extends DeviceDescriptorHandler<List<MutableDevice>> {

        public static final ELEMENT EL = ELEMENT.deviceList;

        public DeviceListHandler(List<MutableDevice> instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
            if (element.equals(DeviceHandler.EL)) {
                MutableDevice device = new MutableDevice();
                getInstance().add(device);
                new DeviceHandler(device, this);
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class DeviceDescriptorHandler<I> extends SAXParser.Handler<I> {

        public DeviceDescriptorHandler(I instance) {
            super(instance);
        }

        public DeviceDescriptorHandler(I instance, SAXParser parser) {
            super(instance, parser);
        }

        public DeviceDescriptorHandler(I instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        public DeviceDescriptorHandler(I instance, SAXParser parser, DeviceDescriptorHandler parent) {
            super(instance, parser, parent);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            ELEMENT el = ELEMENT.valueOrNullOf(localName);
            if (el == null) return;
            startElement(el, attributes);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(uri, localName, qName);
            ELEMENT el = ELEMENT.valueOrNullOf(localName);
            if (el == null) return;
            endElement(el);
        }

        @Override
        protected boolean isLastElement(String uri, String localName, String qName) {
            ELEMENT el = ELEMENT.valueOrNullOf(localName);
            return el != null && isLastElement(el);
        }

        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {

        }

        public void endElement(ELEMENT element) throws SAXException {

        }

        public boolean isLastElement(ELEMENT element) {
            return false;
        }
    }
}
