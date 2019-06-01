package com.example.jz.aprotect;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RefInvoke {
    public static Object invokeStaticMethod(String className,String methodName,Class[] pareType,Object[] pareValues){
        try{
            Class objClass = Class.forName(className);
            Method method = objClass.getMethod(methodName,pareType);
            return method.invoke(null,pareValues);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Object invokeMethod(String className,String methodName,Object obj,Class[] pareType,Object[] pareValues){
        try{
            Class objClass = Class.forName(className);
            Method method = objClass.getMethod(methodName,pareType);
            return method.invoke(obj,pareValues);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object getFieldObject(String className,Object obj,String fieldName){
        try{
            Class objClass = Class.forName(className);
            Field field = objClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static Object getStaticFieldObject(String className,String fieldName)
    {
        try{
            Class objClass = Class.forName(className);
            Field field = objClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static void setFieldObject(String className,String fieldName,Object obj,Object fieldValue){
        try{
            Class objClass = Class.forName(className);
            Field field = objClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj,fieldValue);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    public static void setStaticObject(String className,String fieldName,Object fieldValue){
        try{
            Class objClass = Class.forName(className);
            Field field = objClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null,fieldValue);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
