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
		//1��ɨ��ָ���İ������ð��µ������ļ���·��+���ƴ洢��classNames��
		scanPackage("com.shy");
		
		for(String className:classNames){
			System.out.println(className);
		}
		
		//2����@Service��Controller���е�����ʵ����
		System.out.println("-----------instance-------------");
		this.instance();
		for(Entry<String, Object> entry:beans.entrySet()){
			System.out.println(entry.getKey() + "---------" +entry.getValue());
		}

		System.out.println("-----------ioc-------------");
		//3����controller��������service��������ע��
		this.ioc();
		
		System.out.println("-----------handleMapping-------------");
		//4������url��controller��method��ӳ���ϵ
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
		
		//����·�� consult/test/eat
		String url = request.getRequestURI();
		
		//�������� consult
		String context = request.getContextPath();
		
		//���������ƴӷ���·����ɾ�� path = /test/eat
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
	 * ����url��controller��method��ӳ���ϵ
	 */
	private void handleMapping(){
		if(beans.size() < 1){
			return;
		}
		
		//ѭ���������е�Controller
		for(Map.Entry<String, Object> entry:beans.entrySet()){
			//��ȡController��ʵ��
			Object instance = entry.getValue();
			Class<? extends Object> clazz = instance.getClass();
			if(clazz.isAnnotationPresent(Controller.class)){
				RequestMapping rm = (RequestMapping)clazz.getAnnotation(RequestMapping.class);
				//��ȡ�����ж����RequestMapping��ֵ
				String classUrl = rm.value();
				
				Method[] methods = clazz.getMethods();
				//��ӳ���ϵ����handleMap
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
	 * ��controller��������service��������ע��
	 */
	private void ioc(){
		if(beans.size() < 1){
			return;
		}
		
		//ѭ���������еĶ���
		for(Map.Entry<String, Object> entry:beans.entrySet()){
			//��ȡController��ʵ��
			Object instance = entry.getValue();
			Class<? extends Object> clazz = instance.getClass();
			
			//��ȡController������б�
			Field[] fields = clazz.getDeclaredFields();
			
			for(Field filed:fields){
				//ѭ�������������ж��Ƿ���Qualifier
				//�������Qualifier���򽫸����������ע��
				if(filed.isAnnotationPresent(Qualifier.class)){
					//��ȡQualifier��Controller�ж���ı�����
					Qualifier qualifier = (Qualifier) filed.getAnnotation(Qualifier.class);
					String value = qualifier.value();
					filed.setAccessible(true);
					
					System.out.println("----qualifierName------" + value);
					
					try {
						//����Controller�ж���ı�������benas�л�ȡService��ʵ����������ʵ��ע�뵽Controller��
						//����ȷ��������ע���ʼ���ǵ�ʵ��
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
				
				//�ж����Ƿ���Controller��ע��
				if(clazz.isAnnotationPresent(Controller.class)){
					//Ϊ������map��ָ���ַ�����Զ���for�н��б�������
					//Controller controller = (Controller)clazz.getAnnotation(Controller.class);
					Object instance = clazz.newInstance();
					RequestMapping rm = (RequestMapping)clazz.getAnnotation(RequestMapping.class);
					//��ȡ�����ж����RequestMapping��ֵ
					String rmValue = rm.value();
					beans.put(rmValue, instance);
				}
				
				//�ж����Ƿ���Service��ע��
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
	 * ɨ��ָ���İ�
	 * @param packageName
	 */
	private void scanPackage(String packageName){
		//url��ȡ������·����C:\Users\shyeang\workspace\MavenSpringTest\src\main\java\com\shy
		URL url =  this.getClass().getClassLoader().getResource(replaceTo(packageName));
		String strFile = url.getFile();
		System.out.println(strFile);
		
		File file = new java.io.File(strFile);
		//��ȡ·�������е��ļ��������ļ��У�
		//ѭ��������·���µ������ļ��������ļ��У�
		//������ļ�������ӵ�classNames���list��
		//������ļ��У���ʹ�õݹ�ѭ�������ļ���		
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
