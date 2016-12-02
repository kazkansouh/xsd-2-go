xsd-2-go
========

Given a XSD (XML Schema Definition) file, generating corresponding
Golang XML structs.  This tool is written in Java, and uses reflection
to extract the XML schema from the XJC generated classes.

This is an a fork to add support for more complex schemas, where
namespaces are important and abstract data types are used. This is by
no means a complete library, try at your own risk.

1. Requirements
	* Java 1.6 or above.
	* XJC (Jaxb Binding Compiler) 

2. Usage
    1. Use `xjc` to build java files of your schema into directory `/path/to/dir`
    2. Clone repo
    3. Go into repo and compile with `mvn compile` 
    4. Run `java -cp target/classes/:/path/to/dir main.Start /path/to/dir`

For example, it will output some Golang structs with XML tags.
```go
type XMLVendor struct {
	XMLName xml.Name 	`xml:"vendor"`
	Value	[]string	`xml:"value"`
}

type XMLVersion struct {
	XMLName xml.Name 	`xml:"version"`
	Id	string			`xml:"id,attr"`
	Value	bool		`xml:"value,attr"`
}
```


