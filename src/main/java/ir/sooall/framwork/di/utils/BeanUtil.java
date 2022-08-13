package ir.sooall.framwork.di.utils;

import ir.sooall.framwork.di.Configor;
import ir.sooall.framwork.di.annotations.Bean;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;


public class BeanUtil {

    public record ContextAndMethodHandle(Object context, MethodHandle handle) {
    }

    private BeanUtil() {
        super();
    }

    public static List<ContextAndMethodHandle> handlers(Collection<Class<?>> clazzes) throws Throwable {
        var publicLookup = MethodHandles.publicLookup();
        var flattenMethodHandlers = new ArrayList<ContextAndMethodHandle>();
        for (var clazz : clazzes) {
            var instance = clazz.getDeclaredConstructor().newInstance();
            var methodHandlers = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Bean.class))
                .map(method -> {
                    var type = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
                    var name = method.getName();
                    try {
                        return new ContextAndMethodHandle(instance, publicLookup.findVirtual(clazz, name, type));
                    } catch (NoSuchMethodException | IllegalAccessException e) {
                        e.printStackTrace();
                        return null;
                    }

                })
                .filter(Objects::nonNull).toList();
            flattenMethodHandlers.addAll(methodHandlers);
        }
        return flattenMethodHandlers;

    }


    public static void resolve(Configor configor, ContextAndMethodHandle contextAndMethodHandle) throws Throwable {
        System.out.println("BeanUtil >> resolve >> " + contextAndMethodHandle);
        var handle = contextAndMethodHandle.handle();
        var resolvedObject = configor.getBeanInstance(handle.type().returnType());
        System.out.println("BeanUtil >> resolve >> resolvedObjects " + resolvedObject);
        if (resolvedObject == null) {
            List<Object> resolvedObjects = new ArrayList<>();
            List<Class<?>> unresolvedParams = new ArrayList<>();
            var paramList = handle.type().parameterList();
            for (var param : paramList.subList(1, paramList.size())) {
                if (configor.applicationScope.containsKey(param)) {
                    resolvedObjects.add(configor.applicationScope.get(param));
                } else {
                    unresolvedParams.add(param);
                }
            }
            System.out.println("BeanUtil >> resolve >> resolvedObjects ");
            resolvedObjects.forEach(System.out::println);
            System.out.println("BeanUtil >> resolve >> unresolvedParams ");
            unresolvedParams.forEach(System.out::println);

            System.out.println("BeanUtil >> resolve >> resolvedObjects.size() != handle.type().parameterCount() - 1 : "
                + (resolvedObjects.size() != handle.type().parameterCount() - 1));
            if (resolvedObjects.size() != handle.type().parameterCount() - 1) {
                for (var param : unresolvedParams) {
                    System.out.println("-----------recursive call begin---------------");
                    resolve(configor, configor.handleMap.get(param));
                    resolvedObjects.add(configor.applicationScope.get(param));
                    System.out.println("-----------recursive call end-----------------");
//                    configor.applicationScope.put(handle.type().returnType(), result);

                }
            }
            resolvedObjects.forEach(System.out::println);
            System.out.println(handle.type().returnType());
            System.out.println(contextAndMethodHandle.context());
            Object[] args = new Object[resolvedObjects.size() + 1];
            args[0] = contextAndMethodHandle.context();
            for (int i = 1; i < args.length; i++) {
                args[i] = resolvedObjects.get(i - 1);
            }
            var result = handle.invokeWithArguments(args);
            System.out.println(result);
            configor.applicationScope.put(handle.type().returnType(), result);

//                return result;
        }
//        return resolvedObject;

    }

}
