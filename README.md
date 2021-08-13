# UtilTransformer
Little (java) project useful for mapping JPA entities to Data transer objects. Way much better and dynamic than creating lots of mapper

 * @author kevin.ojeda
 * Class made out of a generic, responsible for passing attributes values between objects through setters and getters methods by reflection. 
 * Supports:
 * -Infinite inherited classes
 * -List(interface) attributes not necessarily of primitive wrappers type but custom beans 
 * -Set(interface) attributes not necessarily of primitive wrappers type but custom beans
 * Rules:
 * -Call the same the variables you need to refer each other 
 * -Don´t use primitives variables, but their wrapper classes
 * -Respect bean encapsulation concept
