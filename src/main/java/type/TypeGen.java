package type;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;

import jaxb.JaxbContext;
import jaxb.JaxbFactory;
import util.Util;

public class TypeGen {

	TypeConvertor typeConvertor;
	Set<String> enumType;

	public TypeGen() {

	}

	public void setTypeConv(TypeConvertor tc) {
		this.typeConvertor = tc;
	}

	public void setEnumTypes(Set<String> e) {
		this.enumType = e;
	}

	boolean isEnumClass(Class cls) {
		String structName = Util.genGoStructName(cls.getName());
		return this.enumType.contains(structName);
	}

	public String typeOf(Field field) {
		String type = field.getType().getName();

		// Field is primitive type
		if (this.typeConvertor.isPrimitive(type)) {
			return this.typeConvertor.getGoType(type);
		}

		// Field is List
		if (field.getGenericType() instanceof ParameterizedType) {
			ParameterizedType param = (ParameterizedType) field.getGenericType();

			if (!field.getType().getName().equals("java.util.List")) {
				System.err.println("can not map to Go A type : " + field.getType());
				System.exit(0);
			}

			Type p = param.getActualTypeArguments()[0];
			Class pClass = (Class) p;

			// Enum Class
			if (isEnumClass(pClass)) {
				return "[]string";
			}

			String goType = this.typeConvertor.getGoType(pClass.getName());
			if (goType == null) {
				System.err.println("can not map to Go B type : " + pClass.getName());
				System.exit(0);
			}

			// builtin type
			if (this.typeConvertor.isPrimitive(pClass.getName())) {
				return "[]" + goType;
			}

			// struct type
			return "[]" + "XML" + goType;
		}

		// Field is Struct
		String goType = field.getType().getName();
		//System.out.println("JJJ: " + goType);
		if (isEnumClass(field.getType())) {
		    return "string";
		}
		goType = this.typeConvertor.getGoType(goType);
		if (goType == null) {
			System.err.println("can not map to Go C type : " + goType + ", field name: "+field.getName());
			System.exit(0);
		}

		return "*" + "XML" + goType;
	}

	public String typeOf(XmlElement element, List<JaxbFactory> jaxbFactories) {

		String refType = element.type().getName();
		// builtin type
		boolean isBuiltin = this.typeConvertor.isPrimitive(refType);
		if (isBuiltin) {
			return this.typeConvertor.getGoType(refType);
		}

		// struct type
		String goType = this.typeConvertor.getGoType(refType);
		if (goType != null) {
			
			return "[]" + "XML" + goType;
		}

		// jaxb type
		if (refType.equals("javax.xml.bind.JAXBElement")) {
		    String qname = element.name();
		    for (JaxbFactory jaxbFactory : jaxbFactories) {
			Class paramClass = jaxbFactory.getJaxbElementType(qname);
			if (paramClass != null) {
			    goType = this.typeConvertor.getGoType(paramClass.getName());
			    if (goType != null) {
				if (this.typeConvertor.isPrimitive(paramClass.getName())) {
				    return goType;
				}
				return "[]" + "XML" + goType;
			    }
			}
		    }
		    System.err.println("can not map to Go Type: " + qname);
		    return null;
		}

		System.err.println("can not map to Go type: " + refType);
		return null;
	}

	public String typeOf(XmlElementRef ref, JaxbContext ctx, List<JaxbFactory> jaxbFactories) {
		String refType = ref.type().getName();

		// builtin type
		boolean isBuiltin = this.typeConvertor.isPrimitive(refType);
		if (isBuiltin) {
			return this.typeConvertor.getGoType(refType);
		}

		// struct type
		String goType = this.typeConvertor.getGoType(refType);
		if (goType != null) {
			return "[]" + "XML" + goType;
		}

		// jaxb type
		if (refType.equals("javax.xml.bind.JAXBElement")) {
			String qname = ref.name();
			for (JaxbFactory jaxbFactory : jaxbFactories) {
			    Class paramClass = jaxbFactory.getJaxbElementType(qname);
			    if (paramClass != null) {
				goType = this.typeConvertor.getGoType(paramClass.getName());
				if (goType != null) {
				    if (this.typeConvertor.isPrimitive(paramClass.getName())) {
					return goType;
				    }

				    // recursive definition
				    if (ctx.isRecursive(goType)) {
					return "[]" + "XML" + goType;
				    }

				    return "*" + "XML" + goType;
				}
			    }
			}
			System.err.println("can not map to Go Type: " + qname);
			return null;
		}

		System.err.println("can not map to Go Type: " + ref.type());
		return null;
	}

}
