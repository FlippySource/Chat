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

package org.teleal.cling.test.resources;

import org.teleal.cling.binding.xml.ServiceDescriptorBinder;
import org.teleal.cling.binding.xml.UDA10ServiceDescriptorBinderImpl;
import org.teleal.cling.binding.xml.UDA10ServiceDescriptorBinderSAXImpl;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.RemoteService;
import org.teleal.cling.test.data.SampleData;
import org.teleal.cling.test.data.SampleServiceOne;
import org.teleal.common.io.IO;
import org.testng.annotations.Test;


public class UDA10ServiceDescriptorParsingTest {

    @Test
    public void readUDA10DescriptorDOM() throws Exception {

        ServiceDescriptorBinder binder = new UDA10ServiceDescriptorBinderImpl();

        RemoteService service = SampleData.createUndescribedRemoteService();

        service = binder.describe(service, IO.readLines(getClass().getResourceAsStream("/test-svc-uda10-one.xml")));

        SampleServiceOne.assertMatch(service, SampleData.getFirstService(SampleData.createRemoteDevice()));
    }

    @Test
    public void readUDA10DescriptorSAX() throws Exception {

        ServiceDescriptorBinder binder = new UDA10ServiceDescriptorBinderSAXImpl();

        RemoteService service = SampleData.createUndescribedRemoteService();

        service = binder.describe(service, IO.readLines(getClass().getResourceAsStream("/test-svc-uda10-one.xml")));

        SampleServiceOne.assertMatch(service, SampleData.getFirstService(SampleData.createRemoteDevice()));
    }

    @Test
    public void writeUDA10Descriptor() throws Exception {

        ServiceDescriptorBinder binder = new UDA10ServiceDescriptorBinderImpl();

        RemoteDevice rd = SampleData.createRemoteDevice();
        String descriptorXml = binder.generate(SampleData.getFirstService(rd));

/*
        System.out.println("#######################################################################################");
        System.out.println(descriptorXml);
        System.out.println("#######################################################################################");

*/

        RemoteService service = SampleData.createUndescribedRemoteService();
        service = binder.describe(service, descriptorXml);
        SampleServiceOne.assertMatch(service, SampleData.getFirstService(rd));
    }

}
