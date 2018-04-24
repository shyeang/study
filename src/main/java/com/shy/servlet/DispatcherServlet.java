package com.shy.servlet;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.shy.annotation.Controller;
import com.shy.annotation.Qualifier;
import com.shy.annotation.RequestMapping;
import com.shy.annotation.Service;
import sun.misc.ProxyGenerator;

/**
 * Servlet implementation class DispatcherServlet
 */
public class DispatcherServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private List<String> classNames = new ArrayList<String>();
	
	private Map<String, Object> beans = new HashMap<String, Object>();
	
	private Map<String, Method> handleMap = new HashMap<String, Method>();
	
	@Override
	public void init(ServletConfig config)  throws ServletException	{
		//1）扫描指定的包，将该包下的所有文件的路径+名称存储到classNames中
		scanPackage("com.shy");
		
		for(String className:classNames){
			System.out.println(className);
		}
		
		//2）对@Service，Controller进行单例的实例化
		System.out.println("-----------instance-------------");
		this.instance();
		for(Entry<String, Object> entry:beans.entrySet()){
			System.out.println(entry.getKey() + "---------" +entry.getValue());
		}

		System.out.println("-----------ioc-------------");
		//3）对controller中依赖的service进行依赖注入
		this.ioc();
		
		System.out.println("-----------handleMapping-------------");
		//4）建立url与controller中method的映射关系
		this.handleMapping();
		for(Entry<String, Method> entry:handleMap.entrySet()){
			System.out.println(entry.getKey() + "---------" +entry.getValue());
		}

		super.init(config);
	}
	
    
 /**
  * @see HttpServlet#HttpServlet()
  */
 public DispatcherServlet() {
     super();
     // TODO Auto-generated constructor stub
 }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		//response.getWriter().append("Served at: ").append(request.getContextPath());
		
		//访问路径 consult/test/eat
		String url = request.getRequestURI();
		
		//工程名称 consult
		String context = request.getContextPath();
		
		//将工程名称从访问路径中删除 path = /test/eat
		String path = url.replace(context, "");
		
		Method method = handleMap.get(path);
		
		//"/" + path.split("/") = /test
		Object instance = beans.get("/" + path.split("/")[1]);
		
		try {
			Object object =  method.invoke(instance, null);
			System.out.println(instance.getClass().getName()+ ":" + object);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

	
	/**
	 * 建立url与controller中method的映射关系
	 */
	private void handleMapping(){
		if(beans.size() < 1){
			return;
		}
		
		//循环遍历所有的Controller
		for(Map.Entry<String, Object> entry:beans.entrySet()){
			//获取Controller的实例
			Object instance = entry.getValue();
			Class<? extends Object> clazz = instance.getClass();
			if(clazz.isAnnotationPresent(Controller.class)){
				RequestMapping rm = (RequestMapping)clazz.getAnnotation(RequestMapping.class);
				//获取在类中定义的RequestMapping的值
				String classUrl = rm.value();
				
				Method[] methods = clazz.getMethods();
				//将映射关系存入handleMap
				for(Method method:methods){
					if(method.isAnnotationPresent(RequestMapping.class)){
						RequestMapping rmMethod = (RequestMapping)method.getAnnotation(RequestMapping.class);
						String methodUrl = rmMethod.value();
						//key:test/eat
						//value:method
						handleMap.put(classUrl + methodUrl, method);
					}
				}
			}
		}
			
	}
	
	
	
	
	/**
	 * 对controller中依赖的service进行依赖注入
	 */
	private void ioc(){
		if(beans.size() < 1){
			return;
		}
		
		//循环遍历所有的对象
		for(Map.Entry<String, Object> entry:beans.entrySet()){
			//获取Controller的实例
			Object instance = entry.getValue();
			Class<? extends Object> clazz = instance.getClass();
			
			//获取Controller的域的列表
			Field[] fields = clazz.getDeclaredFields();
			
			for(Field filed:fields){
				//循环遍历所有域，判断是否是Qualifier
				//如果域是Qualifier，则将该域进行依赖注入
				if(filed.isAnnotationPresent(Qualifier.class)){
					//获取Qualifier在Controller中定义的变量名
					Qualifier qualifier = (Qualifier) filed.getAnnotation(Qualifier.class);
					String value = qualifier.value();
					filed.setAccessible(true);
					
					System.out.println("----qualifierName------" + value);
					
					try {
						//根据Controller中定义的变量名从benas中获取Service的实例，并将该实例注入到Controller中
						//这样确保了依赖注入的始终是单实例
						filed.set(instance, beans.get(value));
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private void instance() {
		if(this.classNames.size() < 1){
			return;
		}
		
		String cnName ;

		for(String className:classNames){
			cnName = className.replace(".class", "");
			
			try {
				Class<?> clazz = Class.forName(cnName);
				
				//判断类是否有Controller的注解
				if(clazz.isAnnotationPresent(Controller.class)){
					//为避免在map中指向地址，所以都在for中进行变量定义
					//Controller controller = (Controller)clazz.getAnnotation(Controller.class);
					Object instance = clazz.newInstance();
					RequestMapping rm = (RequestMapping)clazz.getAnnotation(RequestMapping.class);
					//获取在类中定义的RequestMapping的值
					String rmValue = rm.value();
					beans.put(rmValue, instance);
				}
				
				//判断类是否有Service的注解
				if(clazz.isAnnotationPresent(Service.class)){
					Object instance = clazz.newInstance();
					Service service = (Service)clazz.getAnnotation(Service.class);
					String sValue = service.value();
					beans.put(sValue, instance);
				}
				
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}

	
	/**
	 * 扫描指定的包
	 * @param packageName
	 */
	private void scanPackage(String packageName){
		//url获取到绝对路径：C:\Users\shyeang\workspace\MavenSpringTest\src\main\java\com\shy
		URL url =  this.getClass().getClassLoader().getResource(replaceTo(packageName));
		String strFile = url.getFile();
		System.out.println(strFile);
		
		File file = new java.io.File(strFile);
		//获取路径下所有的文件（包括文件夹）
		//循环变量该路径下的所有文件（包括文件夹）
		//如果是文件，则添加到classNames这个list中
		//如果是文件夹，则使用递归循环该子文件夹		
		String[] strFiles =  file.list();
		
		File singleFile;
		for(String path:strFiles){
			singleFile = new File(strFile + path);
			
			if(singleFile.isDirectory()){
				scanPackage(packageName + "." + path);
			} else{
				classNames.add(packageName + "." + singleFile.getName());
			}
		}
		
	}
	
	private String replaceTo(String packageName){
		return packageName.replace(".", "/");
	}
	
	  public static void main(String[] args) {
		  String s = ".";
		  System.out.println(s.indexOf("."));
		  
		  
	    }
}
