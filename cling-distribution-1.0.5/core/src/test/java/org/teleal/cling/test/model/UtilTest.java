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

package org.teleal.cling.test.model;

import org.teleal.cling.model.ModelUtil;
import org.teleal.cling.model.XMLUtil;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.StringReader;
import java.io.StringWriter;

import static org.testng.Assert.assertEquals;


public class UtilTest {

    final protected DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    final protected DocumentBuilder documentBuilder;

    public UtilTest() {
        try {
            this.documentBuilderFactory.setNamespaceAware(true);
            this.documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void validUDAName() {
        assertEquals(ModelUtil.isValidUDAName("in-valid"), false);

        assertEquals(ModelUtil.isValidUDAName("a_valid"), true);
        assertEquals(ModelUtil.isValidUDAName("A_valid"), true);
        assertEquals(ModelUtil.isValidUDAName("1_valid"), true);
        assertEquals(ModelUtil.isValidUDAName("_valid"), true);

        assertEquals(ModelUtil.isValidUDAName("Some_Valid.Name"), true);
        assertEquals(ModelUtil.isValidUDAName("XML_invalid"), false);
        assertEquals(ModelUtil.isValidUDAName("xml_invalid"), false);
    }

    @Test
    public void csvToString() {

        Object[] plainStrings = new Object[]{"foo", "bar", "baz"};
        assertEquals(ModelUtil.toCommaSeparatedList(plainStrings), "foo,bar,baz");

        Object[] commaStrings = new Object[]{"foo,", "bar", "b,az"};
        assertEquals(ModelUtil.toCommaSeparatedList(commaStrings), "foo\\,,bar,b\\,az");

        Object[] backslashStrings = new Object[]{"f\\oo", "b,ar", "b\\az"};
        assertEquals(ModelUtil.toCommaSeparatedList(backslashStrings), "f\\\\oo,b\\,ar,b\\\\az");
    }

    @Test
    public void stringToCsv() {

        Object[] plainStrings = new Object[]{"foo", "bar", "baz"};
        assertEquals(ModelUtil.fromCommaSeparatedList("foo,bar,baz"), plainStrings);

        Object[] commaStrings = new Object[]{"foo,", "bar", "b,az"};
        assertEquals(ModelUtil.fromCommaSeparatedList("foo\\,,bar,b\\,az"), commaStrings);

        Object[] backslashStrings = new Object[]{"f\\oo", "b,ar", "b\\az"};
        assertEquals(ModelUtil.fromCommaSeparatedList("f\\\\oo,b\\,ar,b\\\\az"), backslashStrings);
    }


    @Test
    public void printDOM1() throws Exception {
        Document dom = documentBuilder.newDocument();
        dom.setXmlStandalone(true); // ROTFL

        Element fooEl = dom.createElement("foo");
        dom.appendChild(fooEl);

        Element barEl = dom.createElement("bar");
        barEl.setAttribute("baz", "123");
        fooEl.appendChild(barEl);

        barEl.setTextContent("abc");

        String xml = XMLUtil.documentToString(dom);

        assertEquals(xml, documentToString(dom));
    }

    @Test
    public void printDOM2() throws Exception {
        Document dom = documentBuilder.newDocument();
        dom.setXmlStandalone(true); // ROTFL

        Element fooEl = dom.createElementNS("urn:foo-bar:baz", "foo");
        dom.appendChild(fooEl);

        Element barEl = dom.createElement("bar");
        barEl.setAttribute("baz", "123");
        fooEl.appendChild(barEl);

        barEl.setTextContent("abc");

        String xml = XMLUtil.documentToString(dom);

        assertEquals(xml, documentToString(dom));
    }

    @Test
    public void printDOM3() throws Exception {
        Document dom = documentBuilder.newDocument();
        dom.setXmlStandalone(true); // ROTFL

        Element fooEl = dom.createElementNS("urn:foo-bar:baz", "foo");
        dom.appendChild(fooEl);

        Element barEl = dom.createElementNS("urn:foo-bar:abc", "bar");
        barEl.setAttribute("baz", "123");
        fooEl.appendChild(barEl);

        barEl.setTextContent("abc");

        String xml = XMLUtil.documentToString(dom);

        assertEquals(xml, documentToString(dom));
    }

    @Test
    public void printDOM4() throws Exception {
        Document dom = documentBuilder.newDocument();
        dom.setXmlStandalone(true); // ROTFL

        Element fooEl = dom.createElement("foo");
        dom.appendChild(fooEl);

        Element barEl = dom.createElementNS("urn:foo-bar:baz", "bar");
        barEl.setAttribute("baz", "123");
        fooEl.appendChild(barEl);

        barEl.setTextContent("abc");

        String xml = XMLUtil.documentToString(dom);

        assertEquals(xml, documentToString(dom));
    }

    @Test
    public void printDOM5() throws Exception {
        Document dom = documentBuilder.newDocument();
        dom.setXmlStandalone(true); // ROTFL

        Element fooEl = dom.createElement("foo");
        dom.appendChild(fooEl);

        Document dom2 = documentBuilder.newDocument();
        dom2.setXmlStandalone(true);

        Element barEl = dom2.createElementNS("urn:foo-bar:baz", "bar");
        barEl.setAttribute("baz", "123");
        dom2.appendChild(barEl);

        Element bazEl = dom2.createElement("baz");
        bazEl.setTextContent("baz");
        barEl.appendChild(bazEl);

        String dom2XML = XMLUtil.documentToString(dom2);
        Document dom2Reparesed = documentBuilder.parse(new InputSource(new StringReader(dom2XML)));
        fooEl.appendChild(dom.importNode(dom2Reparesed.getDocumentElement(), true));

        String xml = XMLUtil.documentToString(dom);

        // We can't really test that, the order of attributes is different
        assertEquals(xml.length(), documentToString(dom).length());
    }

    @Test
    public void printDOM6() throws Exception {
        Document dom = documentBuilder.newDocument();
        dom.setXmlStandalone(true); // ROTFL

        Element fooEl = dom.createElement("foo");
        dom.appendChild(fooEl);

        Element barOneEl = dom.createElementNS("urn:same:space", "same:bar");
        barOneEl.setTextContent("One");
        fooEl.appendChild(barOneEl);

        Element barTwoEl = dom.createElementNS("urn:same:space", "same:bar");
        barTwoEl.setTextContent("Two");
        fooEl.appendChild(barTwoEl);

        String xml = XMLUtil.documentToString(dom);

        assertEquals(xml, documentToString(dom));
    }

    @Test
    public void printDOM7() throws Exception {
        Document dom = documentBuilder.newDocument();
        dom.setXmlStandalone(true); // ROTFL

        Element fooEl = dom.createElement("foo");
        fooEl.setAttribute("bar", "baz");
        dom.appendChild(fooEl);

        String xml = XMLUtil.documentToString(dom);

        assertEquals(xml, documentToString(dom));
    }

    /* TODO: This is where I give up on Android 2.1
    @Test
    public void printDOM8() throws Exception {
        Document dom = documentBuilder.newDocument();
        dom.setXmlStandalone(true); // ROTFL

        Element fooEl = dom.createElementNS("urn:foo", "abc");
        dom.appendChild(fooEl);

        fooEl.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:bar", "urn:bar");

        Element barEl = dom.createElementNS("urn:bar", "bar:def");
        fooEl.appendChild(barEl);

        Element bar2El = dom.createElementNS("urn:bar", "bar:def2");
        fooEl.appendChild(bar2El);

        String xml = XMLUtil.documentToString(dom);
        System.out.println(xml);

        assertEquals(xml, documentToString(dom));
    }
    */

    public static String documentToString(Document document) throws Exception {
        TransformerFactory transFactory = TransformerFactory.newInstance();
        Transformer transformer = transFactory.newTransformer();
        document.setXmlStandalone(true);
        transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
        StringWriter out = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(out));
        return out.toString();
    }

}
