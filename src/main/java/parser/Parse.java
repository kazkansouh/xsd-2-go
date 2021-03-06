package parser;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.*;
import javax.xml.bind.JAXBElement;

import java.util.ArrayList;
import java.util.List;

import jaxb.JaxbContext;
import jaxb.JaxbFactory;
import symbol.GoField;
import symbol.JavaField;
import symbol.Node;
import symbol.SymbolTable;
import type.TypeConvertor;
import type.TypeGen;
import util.Util;
import loader.Loader;

public class Parse {

	Loader loader;
	TypeConvertor typeConvertor;
	TypeGen typeGen;
	List<JaxbFactory> jaxbFactories = new ArrayList<JaxbFactory>();
	StringBuilder buffer;
	SymbolTable symbolTable;

	int level = 0;
	boolean verbose = false;

	public Parse(Loader ld) {
		this.loader = ld;
		this.buffer = new StringBuilder();
		this.symbolTable = new SymbolTable();
	}

	public void init() {
		this.typeConvertor = new TypeConvertor();
		this.typeConvertor.setClassNameTable(this.loader.getClassNameTable());

		this.typeGen = new TypeGen();
		this.typeGen.setTypeConv(this.typeConvertor);

		try {
		    
		    for (String classname : this.loader.getClassFullNameTable()) {
			if (classname.endsWith("ObjectFactory")) {
			    Class factory = this.loader.load(classname);
			    this.jaxbFactories.add(new JaxbFactory(factory));
			}
		    }

			this.findEnumClass();

			this.typeGen.setEnumTypes(this.loader.getEnumClassTable());

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void findEnumClass() throws ClassNotFoundException {

		for (String cls : this.loader.getClassFullNameTable()) {
			Class c = this.loader.load(cls);

			// this class is XmlEnum
			XmlEnum xmlEnum = (XmlEnum) c.getAnnotation(XmlEnum.class);
			if (xmlEnum != null) {
			    String name = Util.genGoStructName(cls);
			    this.loader.getEnumClassTable().add(name);
			}
		}
	}

	public void print(String str) {
		if (this.verbose) {
			for (int i = 0; i < level; i++) {
				System.out.print("\t");
			}
			System.out.print(str);
		}
	}

	public void println(String str) {
		if (this.verbose) {
			for (int i = 0; i < level; i++) {
				System.out.print("\t");
			}
			System.out.println(str);
		}
	}

	/**
	 * Pre filter class: 1 Xml Enum Class
	 * 
	 * @param cls
	 * @return
	 */
	public boolean preprocess(Class cls) {
		String structName = Util.genGoStructName(cls.getName());
		if (this.loader.getEnumClassTable().contains(structName)) {
			return false;
		}

		if (structName.endsWith("ObjectFactory")) {
			return false;
		}

		return true;
	}


    public void parse_aux(Class cls) throws ClassNotFoundException {
		Class clsSup = cls.getSuperclass();
		if (clsSup != null) {
		    parse_aux(clsSup);
		}

		// Fields
		Field[] fields = cls.getDeclaredFields();

		JaxbContext ctx = new JaxbContext();
		ctx.setScope(cls);

		for (Field f : fields) {
			Annotation[] annotations = f.getAnnotations();

			// no annotation
			if (annotations.length == 0) {
			    parseXmlElement(f);
			}

			// has annotation
			else {
				for (Annotation ann : annotations) {
					if (ann instanceof XmlAttribute) {
					    parseXmlAttribute(f);
					} else if (ann instanceof XmlElement) {
					    parseXmlElement(f);
					} else if (ann instanceof XmlElementRefs) {
					    parseXmlElementRefs(f, ctx);
					} else if (ann instanceof XmlElements) {
					    parseXmlElements(f);
					} else if (ann instanceof XmlValue) {
					    parseXmlValue(f);
					} else if (ann instanceof XmlTransient) {
					}
				}
			}
		}

    }


    public String parse(Class cls) throws ClassNotFoundException {

		this.buffer.delete(0, this.buffer.length());

		if (!this.preprocess(cls)) {
			return finalizeParse();
		}

		// Struct Name
		parseStructName(cls);

		parse_aux(cls);

		return finalizeParse();
	}

	private String finalizeParse() {
		if (this.buffer.length() != 0) {
			this.buffer.append(Util.formatGoStructFooter());
		}
		this.symbolTable.reset();
		return this.buffer.toString();
	}

	private void regNodeInfo(JavaField j, GoField g) {
		// node info
		Node node = Node.genNode(j, g);
		this.symbolTable.addNode(node);
	}


	public String findNamespace(Class cls) {
	    Package pkginfo = cls.getPackage();
	    Annotation a = pkginfo.getAnnotation(XmlSchema.class);
	    if (a != null) {
		XmlSchema xmlSchema = (XmlSchema)a;
		return xmlSchema.namespace() + " ";
	    } else {
		System.err.println("ERR: not find annotation XmlSchema");
	    }
	    
	    System.exit(1);
	    return "";
	}


	public String findNamespace(Field f) {
	    Annotation a = f.getAnnotation(XmlElement.class);
	    if (a != null) {
		XmlElement xmlElement = (XmlElement)a;
		if (!xmlElement.namespace().startsWith("##")) {
		    return xmlElement.namespace() + " ";
		}
	    }
	    return findNamespace(f.getDeclaringClass());
	}

	/**
	 * Parse struct name & xml name
	 * 
	 * @param cls
	 */
	public String parseStructName(Class cls) {
		// create wrapping struct (if not root)
		Annotation annotation = cls.getAnnotation(XmlRootElement.class);
		String structName = Util.genGoStructName(cls.getName());
		if (annotation == null) {
		    annotation = cls.getAnnotation(XmlType.class);
		    if (annotation != null) {
			XmlType xmlType = (XmlType) annotation;
			if (!xmlType.name().equals("")) {
			    this.println("struct name: " + "EXML" + structName);
			    this.buffer.append(Util.formatGoStructHeader("EXML" + structName));
			    String xmlName = findNamespace(cls) + xmlType.name();
			    this.println("\t" + "XMLName xml.Name " + Util.genXmlTag(xmlName, true));
			    this.buffer.append(Util.formatGoXMLNameField(Util.genXmlTag(xmlName, true)));
			    this.buffer.append("\t" + "XML" + structName + "\n");
			    this.buffer.append(Util.formatGoStructFooter());
			}
		    }
		}

		// Struct name
		this.println("struct name: " + "XML" + structName);

		this.buffer.append(Util.formatGoStructHeader("XML" + structName));
		this.symbolTable.setJavaClassName(cls.getName());
		this.symbolTable.setGOStructName(structName);

		// XMLName
		annotation = cls.getAnnotation(XmlRootElement.class);
		if (annotation != null) {
			XmlRootElement xmlRootElement = (XmlRootElement) annotation;
			String xmlName = findNamespace(cls) + xmlRootElement.name();
			this.println("\t" + "XMLName xml.Name " + Util.genXmlTag(xmlName, true));
			this.buffer.append(Util.formatGoXMLNameField(Util.genXmlTag(xmlName, true)));
		}

		return structName;
	}


	/**
	 * parse XmlAttribute field
	 * 
	 * @param attributeFileds
	 */
	public void parseXmlAttribute(Field f) {

		XmlAttribute attr = f.getAnnotation(XmlAttribute.class);

		// attribute name
		String attrName = attr.name() == null ? f.getName() : attr.name();
		String goFieldName = Util.genGoField(attrName, true);

		// attribute type
		String type = f.getType().getName();
		String goType = this.typeConvertor.getGoType(type);
		if (goType == null) {
			System.err.println("Can not map to Golang type: " + type);
			System.exit(1);
		}

		// attribute tag
		String goXmlTag = Util.genAttrTag(attrName, attr.required());

		// node info
		JavaField jf = JavaField.genJavaField(f, null);
		GoField gf = GoField.genGoField(goFieldName, goType, goXmlTag);
		this.regNodeInfo(jf, gf);

		this.print("\t" + goFieldName + "\t" + goType + "\t" + goXmlTag + "\n");
		this.buffer.append(Util.formatGoField(goFieldName, goType, goXmlTag));

	}

	/**
	 * parse XmlElement field
	 * 
	 * @param f
	 */
	public void parseXmlElement(Field f) {
		// element name
		String elemName = f.getName();
		String goFieldName = Util.genGoField(elemName, true);

		// element type
		String goType = this.typeGen.typeOf(f);

		// element tag
		String goTag = null;
		XmlElement elementAnn = f.getAnnotation(XmlElement.class);

		if (elementAnn == null) {
			goTag = Util.genXmlTag(findNamespace(f) + f.getName(), false);
		} else {
			String tag = elementAnn.name() == null | elementAnn.name().equals("##default") ? f.getName() : elementAnn.name();
			goTag = Util.genXmlTag(findNamespace(f) + tag, elementAnn.required());
		}

		this.print("\t" + goFieldName + "\t" + goType + "\t" + goTag + "\n");

		// node info
		JavaField javaField = JavaField.genJavaField(f, null);
		GoField goField = GoField.genGoField(goFieldName, goType, goTag);
		this.regNodeInfo(javaField, goField);

		this.buffer.append(Util.formatGoField(goFieldName, goType, goTag));
	}

	/**
	 * parse XmlElements field
	 * 
	 * @param field
	 */
	public void parseXmlElements(Field field) {
		XmlElements xmlElements = field.getAnnotation(XmlElements.class);

		if (xmlElements == null) {
			return;
		}

		for (XmlElement element : xmlElements.value()) {
			// element name
			String goName = Util.genGoField(element.name(), true);

			// element type
			String goType = this.typeGen.typeOf(element, this.jaxbFactories);

			// element tag
			String goTag = Util.genXmlTag(findNamespace(field) + element.name(), false);

			this.print("\t" + goName + "\t" + goType + "\t" + goTag + "\n");

			// node info
			JavaField javaField = JavaField.genJavaField(null, element.type());
			GoField goField = GoField.genGoField(goName, goType, goTag);
			this.regNodeInfo(javaField, goField);

			this.buffer.append(Util.formatGoField(goName, goType, goTag));
		}
	}

	/**
	 * parse XmlElementRefs field
	 * 
	 * @param xmlElementRefs
	 * @param ctx
	 */
	public void parseXmlElementRefs(Field field, JaxbContext ctx) {
		XmlElementRefs xmlElementRefs = field.getAnnotation(XmlElementRefs.class);

		if (xmlElementRefs == null) {
			return;
		}

		for (XmlElementRef ref : xmlElementRefs.value()) {

			// ref name
			String goName = Util.genGoField(ref.name(), true);

			// ref tag
			String goTag = Util.genXmlTag(findNamespace(field) + ref.name(), false);

			// ref type
			String goType = this.typeGen.typeOf(ref, ctx, this.jaxbFactories);

			// node info
			JavaField j = JavaField.genJavaField(null, ref.type());
			GoField g = GoField.genGoField(goName, goType, goTag);
			this.regNodeInfo(j, g);

			this.print("\t" + goName + "\t" + goType + "\t" + goTag + "\n");
			this.buffer.append(Util.formatGoField(goName, goType, goTag));
		}
	}

	/**
	 * parse XmlValue field
	 * 
	 * @param field
	 */
	public void parseXmlValue(Field field) {
		// for XmlValue field, the name is 'Value'
		String goName = "Value";

		// type
		String goType = this.typeGen.typeOf(field);

		// for XmlValue field, the tag is 'chardata'
		String goTag = Util.genCharDataTag();

		// node info
		JavaField j = JavaField.genJavaField(field, null);
		GoField g = GoField.genGoField(goName, goType, goTag);
		this.regNodeInfo(j, g);

		this.print("\t" + goName + "\t" + goType + "\t" + goTag + "\n");
		this.buffer.append(Util.formatGoField(goName, goType, goTag));
	}
}
