fasax
=====

The fastest way to unmarshall XML to Java on Android.

Note: This project is far from complete, but can unmarshell simple xml 
documments.

Unlike other implementations, this libary generates SAX parsing code at compile 
time. This makes it's performace virtually the same as a handwritten SAX parser.
Annotations are inspired from [http://simple.sourceforge.net/](Simple XML).

Example
-------

```xml
<enterprise>
 <corporation>Agile Analitics</corporation>
  <software-license name="Proprietary" year="2014"/>
  <software>Cloud Big Data Management</software>
  <software>JVMXMLRPP</software>
  <software>Addition as a Service</software>
  <employees>
    <employee>
      <name>Vendelín Gamil</name>
      <title>Senior Architect</title>
      <date-started>2010-12-01</date-started>
    </employee>
    <employee>
      <name>Huw Andrea</name>
      <title>Junior Architect</title>
      <date-started>2013-06-12</date-started>
    </employee>
    <employee>
      <name>Benjy Teodors</name>
      <title>Head Senior Architect</title>
      <date-started>2011-10-24</date-started>
    </employee>
  </employees>
</enterprise>
```

```java
@Xml
pubic class Enterprise {
    @Element
    public String corperation;

    @Element(name="software-license")
    public SoftwareLicense softwareLicense;

    @ElementList(entry="software", inline=true)
    public List<String> software;

    @ElementList(entry="employee")
    public List<Employee> employees;

    @Xml
    public static class SoftwareLicense {
        @Attribute
        public String name;

        @Attribute
        public String year;
    }

    @Xml
    public static class Employee {
        @Element
        public String name;

        @Element
        public String title;

        @Converter(DateConverter.class)
        @Element(name="date-started")
        public Date dateStarted;
    }
}
```

```java
Fasax fasax = new Fasax();
InputStream in = getXmlInputStream();
Enterprise = fasax.fromXml(in, Enterprise.class);
```

TODO
----
- Allow use of setters instead of public fields
- Write XML
- Namespaces
