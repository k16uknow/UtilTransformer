package com.mx.amlps.util.mapper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UtilTransformer <T> {
	
	private final Class<T> toType;
	
	private Object fromType;
	
	private Object endingObject;

    public UtilTransformer (Class<T> type) {
        this.toType = type;
        this.endingObject = null;
    }
    
    public UtilTransformer (Class<T> type, Object endingObject) {
        this.toType = type;
        this.endingObject = endingObject;
    }
	
	/**
	 * 
	 * @param dto
	 * @return
	 */
	
	public List<T> transformListObjects(List<?> dtos){
		List<T> response = new ArrayList<T>();
		for(Object obj : dtos) {
			if(endingObject == null)
				response.add(new UtilTransformer<T>(toType).transformObject(obj));
			else
				response.add(new UtilTransformer<T>(toType, endingObject).transformObject(obj));
		}
		return response;
	}
	
	/**
	 * 
	 * @param dto
	 * @return
	 */
	
	public Set<T> transformSetObjects(Set<?> dtos){
		Set<T> response = new HashSet<T>();
		if(endingObject == null){
			for(Object obj : dtos) 
				response.add(new UtilTransformer<T>(toType).transformObject(obj));
		}
		else {
			for(Object obj : dtos)
				response.add(new UtilTransformer<T>(toType, endingObject).transformObject(obj));
		}
		return response;
	}
	
	/**
	 * 
	 * 
	 */
	@SuppressWarnings("unchecked")
	public T transformObject(Object dto) {
		this.fromType = dto;
		Map<String, UtilTransformerObject> util = createMethodMap();
		util = completeFromMethodMethodMap(util);
		T respObj = null;
		try {
			respObj = this.toType.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		for(String key : util.keySet()) {
			UtilTransformerObject objectt = util.get(key);
			if(null == objectt.getOrFromMethod || !objectt.returningClass.equals(objectt.parameters[0]) ) {
				if(!isWrapperType(objectt.returningClass) && !isWrapperType(objectt.parameters[0])) {
					try {
						
							Object settedOb = fromType.getClass().getMethod(objectt.getOrFromMethod,  new Class<?>[] {}).invoke(fromType, new Object[] {});

							settedOb = settedOb == endingObject ? null : 
								new UtilTransformer<Object>((Class<Object>) objectt.parameters[0], dto).transformObject(
										settedOb);
							respObj.getClass().getMethod(key, objectt.parameters).invoke(respObj, settedOb);
						
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
						e.printStackTrace();
					}
				}
				continue;
			}
			printObjectInfo(key, objectt);
			if(null != objectt.collectionClass) {
				try {
					Boolean endingObjectFound = false;
					if(objectt.collectionType.equals(UtilTransformerObject.LIST_METHOD)) {
						List<Object> settedObjList = null;
						settedObjList = (List<Object>) fromType.getClass().getMethod(objectt.getOrFromMethod,  new Class<?>[] {}).invoke(fromType, new Object[] {});
						if(null != endingObject) {
							endingObjectFound = settedObjList.stream().anyMatch(x -> x == endingObject);
						}
						respObj.getClass().getMethod(key, objectt.parameters).invoke(respObj,  
							( !endingObjectFound ? 
								new UtilTransformer<Object>((Class<Object>) objectt.collectionClass, dto).transformListObjects(settedObjList) 
								: null
							) 
						);
					}else {
						Set<Object> settedObjList = null; 
						settedObjList = (Set<Object>) fromType.getClass().getMethod(objectt.getOrFromMethod,  new Class<?>[] {}).invoke(fromType, new Object[] {});
						if(null != endingObject) {
							endingObjectFound = settedObjList.stream().anyMatch(x -> x == endingObject);
						}
						respObj.getClass().getMethod(key, objectt.parameters).invoke(respObj, 
							( !endingObjectFound ? 
								(new UtilTransformer<Object>((Class<Object>) objectt.collectionClass, dto).transformSetObjects(settedObjList))
								: null
							)
						);
					}
					
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}
			}else {
				try {
					respObj.getClass().getMethod(key, objectt.parameters).invoke(respObj, 
							(fromType.getClass().getMethod(objectt.getOrFromMethod,  new Class<?>[] {}).invoke(fromType, new Object[] {}))
					);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
						| NoSuchMethodException | SecurityException e) {
					e.printStackTrace();
				}
			}
		}
		return respObj;
	}
	
	private Map<String, UtilTransformerObject> createMethodMap(){
		Map<String, UtilTransformerObject> response = utilTransformerForTargetClass(new HashMap<String, UtilTransformerObject>(), toType);
		Class<?> analizedClass = toType.getSuperclass();
		while(!analizedClass.equals(Object.class)) {
			response = utilTransformerForTargetClass(response, analizedClass);
			analizedClass = analizedClass.getSuperclass();
		}
		return response;
	}
	
	private Map<String, UtilTransformerObject> completeFromMethodMethodMap(Map<String, UtilTransformerObject> mapp) {
		mapp = utilTransformerForSourceClass(mapp, fromType.getClass());
		
		Class<?> analizedClass = fromType.getClass().getSuperclass();
		while(!analizedClass.equals(Object.class)) {
			mapp = utilTransformerForSourceClass(mapp, analizedClass);
			analizedClass = analizedClass.getSuperclass();
		}
		
		return mapp;
	}
	
	private Map<String, UtilTransformerObject> utilTransformerForTargetClass(Map<String, UtilTransformerObject> mapp, Class<?> theClass){
		List<Method> methods =  Arrays.asList(theClass.getDeclaredMethods());
		for(Method m : methods) {
			Optional<Field> f = settingsForVar(m, theClass);
			if(!f.isPresent()) continue;
			Field field = f.get();
			UtilTransformerObject temp = new UtilTransformerObject();
			temp.parameters = new Class[] {field.getType()};
			temp.theVarName = field.getName();
			mapp.put(m.getName(), temp);
			
			if(List.class.equals(field.getType()) || Set.class.equals(field.getType())){
				ParameterizedType pType = (ParameterizedType) field.getGenericType();
				temp.collectionClass = (Class<?>) pType.getActualTypeArguments()[0];
				if(!isWrapperType(temp.collectionClass))
					temp.collectionType = List.class.equals(field.getType()) 
						? UtilTransformerObject.LIST_METHOD 
							: UtilTransformerObject.SET_METHOD;
			}
		}
		return mapp;
	}
	
	private Map<String, UtilTransformerObject> utilTransformerForSourceClass(Map<String, UtilTransformerObject> mapp, Class<?> theClass){
		List<Method> methods =  Arrays.asList(theClass.getDeclaredMethods());
		for(Method m : methods) {
			Optional<Field> f = gettersForVar(m, theClass);
			if(!f.isPresent()) continue;
			Field field = f.get();
			UtilTransformerObject temp = mapp.get("s" + m.getName().substring(1, m.getName().length()));
			if(null == temp) continue;
			temp.getOrFromMethod = m.getName();
			temp.returningClass = field.getType();
		}
		return mapp;
	}
	
	private Optional<Field> settingsForVar(Method method, Class<?> theClass) {
		System.out.println(method.getParameterTypes());
		
		List<Field> fields =  clearSerialVersion(Arrays.asList(theClass.getDeclaredFields()));
		
		for(Field f : fields) {
			String fName = SET_METHODS + f.getName().substring(0,1).toUpperCase() + f.getName().substring(1, f.getName().length()); 
			if (fName.equals(method.getName()) 
				&& method.getReturnType().equals(void.class)
				&& method.getParameterCount() == 1
				&& method.getParameterTypes()[0].equals(f.getType())) 
					return Optional.of(f);
		}
		return Optional.empty();
	}
	
	
	private Optional<Field> gettersForVar(Method method, Class<?> theClass) {
		List<Field> fields =  clearSerialVersion(Arrays.asList(theClass.getDeclaredFields()));
		
		for(Field f : fields) {
			String fName = GET_METHODS + f.getName().substring(0,1).toUpperCase() + f.getName().substring(1, f.getName().length()); 
			if (fName.equals(method.getName())
				&& method.getReturnType().equals(f.getType())
				&& method.getParameterCount() == 0)
					return Optional.of(f);
		}
		return Optional.empty();
	}
	
	private List<Field> clearSerialVersion(List<Field> fields) {
		return fields.stream().filter(x ->{
			return !x.getName().equals("serialVersionUID");
		}).collect(Collectors.toList());
	}
	
	private void printObjectInfo(String setMethod, UtilTransformerObject objectt) {
		System.out.println(
				"Variable: " + objectt.theVarName + 
				", setterMethod: " + setMethod + 
				", getterMethod: " + objectt.getOrFromMethod +
				", return getMethodClass: " + objectt.returningClass.getName() +
				", parameter setMethodClass: " + objectt.parameters[0].getName());
	}
	
	private final static String SET_METHODS = "set";
	
	private final static String GET_METHODS = "get";
	
	private static final Set<Class<?>> WRAPPER_TYPES = getWrapperTypes();

    private boolean isWrapperType(Class<?> clazz)
    {
        return WRAPPER_TYPES.contains(clazz);
    }

    private static Set<Class<?>> getWrapperTypes()
    {
        Set<Class<?>> ret = new HashSet<Class<?>>();
        ret.add(Boolean.class);
        ret.add(Character.class);
        ret.add(Byte.class);
        ret.add(Short.class);
        ret.add(Integer.class);
        ret.add(Long.class);
        ret.add(Float.class);
        ret.add(Double.class);
        ret.add(Void.class);
        return ret;
    }
	
}

