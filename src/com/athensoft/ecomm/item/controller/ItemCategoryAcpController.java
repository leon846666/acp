package com.athensoft.ecomm.item.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.athensoft.content.event.entity.Event;
import com.athensoft.ecomm.item.entity.ItemCategory;
import com.athensoft.ecomm.item.entity.ItemCategoryStatus;
import com.athensoft.ecomm.item.entity.ItemProduct;
import com.athensoft.ecomm.item.service.ItemCategoryService;
import com.athensoft.util.Node;
import com.athensoft.util.excel.ExcelExport;
import com.athensoft.util.locale.LocaleHelper;
import com.athensoft.util.matrix.ArrayHelper;
import com.athensoft.util.tree.ManyNodeTree;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Athens
 *
 */
@Controller
public class ItemCategoryAcpController {
	 
	private static final Logger logger = Logger.getLogger(ItemCategoryAcpController.class);

	/**
	 * ItemCategory Service instance
	 */
	private ItemCategoryService itemCategoryService;
	
	@Autowired
	public void setItemCategoryService(ItemCategoryService itemCategoryService) {
		this.itemCategoryService = itemCategoryService;
	}
	
	/**
	 * go to the view of item category
	 * @return the target view name 
	 */
	@RequestMapping(value="/item/testcategory",produces="application/json")
	@ResponseBody
	public ModelAndView gotoTestCategory(){
		logger.info("entering /item/testcategory");
		
		ModelAndView mav = new ModelAndView();
		
		//view
		String viewName = "item/test_item_category";
		mav.setViewName(viewName);
		
		//data
		Map<String, Object> model = mav.getModel();
		
		//build jstree data
		Node treeRootNode = new Node(null);
	    treeRootNode.setText("Category Classification");
	    treeRootNode.setState(Node.buildList(new AbstractMap.SimpleEntry<String, String>("key", "ROOT")));		//here ROOT is derived from table:item_category
	    String localeStr = LocaleHelper.getLocaleStr();
	    localeStr=LocaleHelper.localToLangNo(localeStr);
	    
	    List<ItemCategory> list = new ArrayList<ItemCategory>();
	    list = this.itemCategoryService.findAll(localeStr);
	    
	    logger.info("list size:= "+list.size());
	    
	    for (ItemCategory ic : list) {
	    	long parent = ic.getParentId();
	    	logger.info("parent_id="+parent);
	    	ItemCategory p = this.itemCategoryService.findByCategoryId(parent,localeStr);
	    	String parentCode = p.getCategoryCode();
	    	logger.info("parent_code="+parentCode);
	    	Node parentNode = Node.getNodeByKey(treeRootNode, parentCode);
	    	logger.info("parentNode.text="+parentNode.getText());
	    	Node.addChild(parentNode, ic.getCategoryName(), Node.buildList(new AbstractMap.SimpleEntry<String, String>("key", ic.getCategoryCode())));
	    }
	    StringBuffer jsTreeData = Node.buildJSTree(treeRootNode, "  ").append("}");
	    logger.info(jsTreeData);
			
		model.put("jsTreeData", "["+jsTreeData.toString()+"]");
				
		logger.info("leaving /item/testcategory");
		return mav;
	}
	
	/**
	 * go to the view of item category create
	 * @return the target view name 
	 */
	@RequestMapping(value="/item/itemCategoryCreate")
	public ModelAndView gotoCategoryCreate(){
		
		logger.info("entering /item/itemCategoryCreate");
		
		ModelAndView mav= new ModelAndView();
		mav.setViewName("item/item_category_create");
		
		logger.info("leaving /item/itemCategoryCreate");
		
		return mav;
	}
	/**
	 * updateProductGroup
	 * */
	@RequestMapping(value="/item/updateCategoryGroup",method=RequestMethod.POST)
	@ResponseBody
	public Map<String,Object> updateProductGroup(
				@RequestParam String categoryIDArray,
				@RequestParam int categoryStatus
				) {
			
			logger.info("entering /item/updateCategoryGroup");
			
			/* initial settings */
			ModelAndView mav = new ModelAndView();
			
			//set model
	        Map<String, Object> model = mav.getModel();
	   
	        ArrayList<ItemCategory> cateList = new ArrayList<ItemCategory>();
	        String[] categoryIDs = categoryIDArray.split(",");
	        int categoryIDLength = categoryIDs.length;
	        
	        String localeStr= LocaleHelper.getLocaleStr();
	        localeStr=  LocaleHelper.localToLangNo(localeStr);
	        
	        for(int i=0; i<categoryIDLength; i++){
	        	ItemCategory category = new ItemCategory();
	        	category.setCategoryId(Long.parseLong(categoryIDs[i]));
	        	category.setCategoryStatus(categoryStatus);
	        	category.setCategoryLangNo(localeStr);
	        	cateList.add(category);
	        	 
	        }
	        
	        logger.info("prodList size="+cateList.size());
	        logger.info("prodList ="+cateList.toString());
	        
			/* business logic*/
	         itemCategoryService.batchUpdateCategory(cateList);
	        
			
			/* assemble model and view */
			logger.info("leaving /item/updateCategoryGroup");
			return model;		
	}
	
	/**
	 * get all the parent category 
	 * @return the data 
	 */
	@RequestMapping(value="/item/getAllCategoryParent")
	@ResponseBody
	public Map<String,Object> getAllCategoryParent(){
		
		logger.info("entering /item/getAllCategoryParent");
		Map<String, Object> model =new HashMap<>();
		
		//build jstree data
		Node treeRootNode = new Node(null);
	    treeRootNode.setText("Category Classification");
	    treeRootNode.setState(Node.buildList(new AbstractMap.SimpleEntry<String, String>("key", "ROOT")));		//here ROOT is derived from table:item_category

	    String localeStr = LocaleHelper.getLocaleStr();
	    localeStr=LocaleHelper.localToLangNo(localeStr);
	    
	    
	    List<ItemCategory> list = new ArrayList<ItemCategory>();
	    list = this.itemCategoryService.findAll(localeStr);
	    
	    logger.info("list size:= "+list.size());
	    
	    for (ItemCategory ic : list) {
	    	long parent = ic.getParentId();
	    	logger.info("parent_id="+parent);
	    	ItemCategory p = this.itemCategoryService.findByCategoryId(parent,localeStr);
	    	//System.out.println(p.toString());
	    	String parentCode = p.getCategoryCode();
	    	logger.info("parent_code="+parentCode);
 
	    	Node parentNode = Node.getNodeByKey(treeRootNode, parentCode);
	    	logger.info("parentNode.text="+parentNode.getText());
	    	Node.addChild(parentNode, ic.getCategoryName(), Node.buildList(new AbstractMap.SimpleEntry<String, String>("key", ic.getCategoryCode())));
	    }
	    StringBuffer jsTreeData = Node.buildJSTree(treeRootNode, "  ").append("}");
	    logger.info(jsTreeData);
			
		model.put("jsTreeData", "["+jsTreeData.toString()+"]");
		logger.info("leaving /item/getAllCategoryParent");
		
		return model;
	}
	
	/**
	 * go to the view of item category
	 * @return the target view name 
	 */
	@RequestMapping(value="/item/categoryListTree",produces="application/json")
	@ResponseBody
	public ModelAndView gotoCategoryListTree(){
		logger.info("entering /item/categoryListTree");
		
		ModelAndView mav = new ModelAndView();
		
	    String localeStr = LocaleHelper.getLocaleStr();
	    localeStr=LocaleHelper.localToLangNo(localeStr);
	    
		//view
		String viewName = "item/item_category_list_tree";
		mav.setViewName(viewName);
		
		//data
		Map<String, Object> model = mav.getModel();
		
		//build jstree data
		Node treeRootNode = new Node(null);
	    treeRootNode.setText("Category Classification");
	    treeRootNode.setState(Node.buildList(new AbstractMap.SimpleEntry<String, String>("key", "ROOT")));		//here ROOT is derived from table:item_category
	 
	    
	    List<ItemCategory> list = new ArrayList<ItemCategory>();
	    list = this.itemCategoryService.findAll(localeStr);
	    
	    logger.info("list size:= "+list.size());
	    
	    for (ItemCategory ic : list) {
	    	long parent = ic.getParentId();
	    	logger.info("parent_id="+parent);
	    	ItemCategory p = this.itemCategoryService.findByCategoryId(parent,localeStr);
	    	String parentCode = p.getCategoryCode();
	    	logger.info("parent_code="+parentCode);
	    	Node parentNode = Node.getNodeByKey(treeRootNode, parentCode);
	    	logger.info("parentNode.text="+parentNode.getText());
	    	Node.addChild(parentNode, ic.getCategoryName(), Node.buildList(new AbstractMap.SimpleEntry<String, String>("key", ic.getCategoryCode())));
	    }
	    StringBuffer jsTreeData = Node.buildJSTree(treeRootNode, "  ").append("}");
	    logger.info(jsTreeData);
			
		model.put("jsTreeData", "["+jsTreeData.toString()+"]");
				
		logger.info("leaving /item/categoryListTree");
		return mav;
	}
	
	/**
	 * go to the view of item category
	 * @return the target view name 
	 */
	@RequestMapping(value="/item/samplecategory")
	public String gotoSampleCategory(){
		String viewName = "item/sample_item_category";
		return viewName;
	}
	
	
	/**
	 * @return
	 * @author Athens Zhang
	 */
	@RequestMapping(value="/item/categoryListData",produces="application/json")
	@ResponseBody
	public Map<String,Object> getDataItemCategoryList(HttpServletRequest request){
		logger.info("entering /item/categoryListData");	
		ModelAndView mav = new ModelAndView();
		//data
		String localeStr = LocaleHelper.getLocaleStr();
	    localeStr=LocaleHelper.localToLangNo(localeStr);
		List<ItemCategory> listCategory = new ArrayList<ItemCategory>();
		listCategory = itemCategoryService.findCategoryTreeByCategoryId(localeStr,1);
		logger.info("Length of news entries: "+ listCategory.size());
		HttpSession session = request.getSession();
	    session.setAttribute("listCategoryByFilter", listCategory);
		
		String[][] data = getData(listCategory);
		
		Map<String, Object> model = mav.getModel();  
		
		model.put("draw", new Integer(1));
		model.put("recordsTotal", new Integer(5));
		model.put("recordsFiltered", new Integer(5));
		model.put("data", data);
		model.put("customActionStatus","OK");
		model.put("customActionMessage","Data loaded");
		
		logger.info("leaving /item/categoryListData");
		return model;
	}
	
	@RequestMapping(value="/item/deleteCategory")
	@ResponseBody
	public Map<String,Object> deleteCategory(@RequestParam String categoryId){
		logger.info("entering /item/deleteCategory");
		
		Map<String,Object> model= new HashMap<String,Object> ();
		int result =this.itemCategoryService.deleteItemCategoryByCategoryId(Long.parseLong(categoryId));
		if(result==1){
			model.put("success", true);
		}else{
			model.put("success", false);
		}
		
		
		logger.info("leaving /item/deleteProduct");
		return model;
	}
	
	@RequestMapping(value="/item/newCreateCategory",method=RequestMethod.POST)
	@ResponseBody
	public Map<String, Object>  createCategory(@RequestBody ItemCategory itemCategory){
	
		logger.info("entering  /item/newCreateCategory");
		/*String localeStr = LocaleHelper.getLocaleStr();
	    localeStr=LocaleHelper.localToLangNo(localeStr);
	    */
		String localeStr=null;
		ModelAndView mav = new ModelAndView();
		//System.out.println(" name "+itemCategory.getCategoryName());
		Map<String, Object> model=mav.getModel();

		int result = itemCategoryService.createCategory(itemCategory,localeStr);
		
		if(result==1){
			model.put("success", true);  
			model.put("msg","ok");
		}else{
			model.put("error", false);
			model.put("msg","no");
		}
		
	
		mav.setViewName("item/categoryList");
		logger.info("leaving /item/newCreateCategory");
	
		return model;
	}
	
	/**
	 * @param itemJSONString
	 * @return
	 * @author Athens
	 */
	@RequestMapping(value="/item/categorySearchFilterData",produces="application/json")
	@ResponseBody
	public Map<String, Object> getDataSearchCategoryByFilter(@RequestParam String itemJSONString,HttpServletRequest request){
		logger.info("entering /item/categorySearchFilterData");
		
		ModelAndView mav = new ModelAndView();
		
		//data
		Map<String, Object> model = mav.getModel();
		JSONObject jobj= new JSONObject();
		ItemCategory itemCategory= jobj.parseObject(itemJSONString, ItemCategory.class);
		
		String localeStr =LocaleHelper.getLocaleStr();
		localeStr=LocaleHelper.localToLangNo(localeStr);
		
		String where1 = itemCategory.getCategoryId()+"";
		String where2 = itemCategory.getParentId()+"";
		String where3 = itemCategory.getCategoryCode();
		String where4 = itemCategory.getCategoryName(); 
		String where5 = itemCategory.getCategoryDesc();
		String where6 = itemCategory.getCategoryLevel()+"";
	  
		StringBuffer queryString = new StringBuffer();
		queryString.append(where1.trim().length()==0||where1.equals("null")?" ":" and ic.category_id like '%"+where1+"%' ");
		queryString.append(where2.trim().length()==0||where2.equals("null")?" ":" and parent_id like '%"+where2+"%' ");
		queryString.append(where3.trim().length()==0?" ":" and category_code like '%"+where3+"%' ");
		queryString.append(where4.trim().length()==0?" ":" and category_name like '%"+where4+"%' ");
		queryString.append(where5.trim().length()==0?" ":" and category_desc like '%"+where5+"%' ");
		queryString.append(where6.trim().length()==0||where6.equals("null")?" ":" and category_level = "+where6+" ");
		//queryString.append(where7==0?" ":" and category_status = "+where7+" ");
		 
		logger.info("QueryString = "+ queryString.toString());
		 
		List<ItemCategory> listCategory = itemCategoryService.getCategoryByFilter(queryString.toString(),localeStr);
		logger.info("Length of ItemCategory entries = "+ listCategory.size());
		  
		HttpSession session = request.getSession();
		session.setAttribute("listCategoryByFilter", listCategory);
		 
		String[][] data = getDataWithoutTree(listCategory);
		
		model.put("draw", new Integer(1));
		model.put("recordsTotal", new Integer(5));
		model.put("recordsFiltered", new Integer(5));
		model.put("data", data);
		model.put("customActionStatus","OK");
		model.put("customActionMessage","OK");
		
		logger.info("leaving /item/categorySearchFilterData");
		
		return model;
	} 
	
	@RequestMapping(value="/item/exportCategoryExcel",method={RequestMethod.GET,RequestMethod.POST})
	public ModelAndView exportCategoryExcel(HttpServletRequest request,HttpServletResponse response) throws Exception{
		 
			String fileName="item_category_list";
			HttpSession session = request.getSession();
			List<ItemCategory> listcategories =(List<ItemCategory>) session.getAttribute("listCategoryByFilter");
			 String localeStr = LocaleHelper.getLocaleStr();
			    localeStr=LocaleHelper.localToLangNo(localeStr);
			    
			if(null==listcategories){
				listcategories = this.itemCategoryService.findAll(localeStr);
			}
	        List<Map<String,Object>> list=createExcelRecord(listcategories);
	        String columnNames[]={"Category ID","Parent ID","Category Code","Category Name",
	        					"Description ","Level ","Status"};
	        String keys[]    =     {"category_id","parent_id","category_code","category_name","category_desc","category_level"
	        		,"category_status"};
	        ByteArrayOutputStream os = new ByteArrayOutputStream();
	        try {
	        	ExcelExport.createWorkBook(list,keys,columnNames).write(os);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        byte[] content = os.toByteArray();
	        InputStream is = new ByteArrayInputStream(content);
	        response.reset();
	        response.setContentType("application/vnd.ms-excel;charset=utf-8");
	        response.setHeader("Content-Disposition", "attachment;filename="+ new String((fileName + ".xls").getBytes(), "utf-8"));
	        ServletOutputStream out = response.getOutputStream();
	        BufferedInputStream bis = null;
	        BufferedOutputStream bos = null;
	        try {
	            bis = new BufferedInputStream(is);
	            bos = new BufferedOutputStream(out);
	            byte[] buff = new byte[2048];
	            int bytesRead;
	            // Simple read/write loop.
	            while (-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
	                bos.write(buff, 0, bytesRead);
	            }
	        } catch (final IOException e) {
	            throw e;
	        } finally {
	            if (bis != null)
	                bis.close();
	            if (bos != null)
	                bos.close();
	        }
	        return null;
	    }
	 
	
	
    private List<Map<String, Object>> createExcelRecord(List<ItemCategory> listCategories) {
        List<Map<String, Object>> listmap = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("sheetName", "sheet1");
        listmap.add(map);
        ItemCategory itemCategory=null;
        for (int j = 0; j < listCategories.size(); j++) {
        	itemCategory=listCategories.get(j);
            Map<String, Object> mapValue = new HashMap<String, Object>();
            mapValue.put("category_id", itemCategory.getCategoryId());
            mapValue.put("category_code", itemCategory.getCategoryCode());
            mapValue.put("category_desc", itemCategory.getCategoryDesc());
            mapValue.put("category_name", itemCategory.getCategoryName());
            mapValue.put("category_level",itemCategory.getCategoryLevel());
            mapValue.put("category_status",itemCategory.getCategoryStatus());
            mapValue.put("parent_id",itemCategory.getParentId());
            listmap.add(mapValue);
        }
        return listmap;
    
}
	
	@RequestMapping(value="/item/dragAndDropResultSaved",method=RequestMethod.POST,produces="application/json")
	@ResponseBody
	public Map<String, Object> dragAndDropResultSaved(@RequestParam String orig, @RequestParam String dest){
		logger.info("entering /item/dragAndDropResultSaved");
		
		ModelAndView mav = new ModelAndView();
		
		//view
		String viewName = "item/testcategory";
		mav.setViewName(viewName);
		
		//service
//		this.itemCategoryService.dragAndDropResultSaved(orig, dest);
		
		//data
		Map<String, Object> model = mav.getModel();
		 String localeStr = LocaleHelper.getLocaleStr();
		  localeStr=LocaleHelper.localToLangNo(localeStr);

		// Save data to DB
		List<ItemCategory> list = new ArrayList<ItemCategory>();
		ItemCategory p = this.itemCategoryService.findByCategoryCode(dest);
		ItemCategory old = this.itemCategoryService.findByCategoryCode(orig);
		int levelDifference = p.getCategoryLevel() - old.getCategoryLevel() + 1;
		list.add(old); //getDesendants does not include this node.
		list = this.getDesendants(list, old.getCategoryId());
		logger.info("p.level="+p.getCategoryLevel()+"  old.level="+old.getCategoryLevel()+"  levelDifference="+levelDifference);

		old.setCategoryCode(p.getCategoryCode());
		old.setCategoryLevel(0);
		old.setParentId(p.getCategoryId());
		this.itemCategoryService.createCategory(old,localeStr); 
		this.itemCategoryService.deleteItemCategoryByCategoryId(old.getCategoryId());
		
		model.put("orig",orig);
		model.put("dest", dest);
		
		logger.info("Orig : " + orig + "      Dest : " + dest);
		
		logger.info("leaving /item/dragAndDropResultSaved");
//		return mav;
		return model;
	}
	
	@RequestMapping(value="/item/createResultSaved",method=RequestMethod.POST,produces="application/json")
	@ResponseBody
	public Map<String, Object> createResultSaved(@RequestParam String parent, @RequestParam String text){
		logger.info("entering /item/createResultSaved");
		
		ModelAndView mav = new ModelAndView();
		
		System.out.println(parent+","+text);
		//view
		String viewName = "item/testcategory";
		mav.setViewName(viewName);
		 String localeStr = LocaleHelper.getLocaleStr();
		  localeStr=LocaleHelper.localToLangNo(localeStr);

		//service
		ItemCategory p = this.itemCategoryService.findByCategoryCode(parent);
    	long parentId = p.getCategoryId();
    	int parentLevel = p.getCategoryLevel();
		String newCategoryCode = this.itemCategoryService.createResultSaved(parentId, text, parentLevel,localeStr);
		
		//data
		Map<String, Object> model = mav.getModel();

		String newKey = newCategoryCode;
		model.put("parent", parent);
		model.put("newKey", newKey);
		
		logger.info("Parent : " + parent + "      Text : " + text + "      New Key : " + newKey);
		
		logger.info("leaving /item/createResultSaved");
		return model;
	}
	
	@RequestMapping(value="/item/renameResultSaved",method=RequestMethod.POST,produces="application/json")
	@ResponseBody
	public Map<String, Object> renameResultSaved(@RequestParam String old, @RequestParam String newText, @RequestParam String key){
		logger.info("entering /item/renameResultSaved");
		
		ModelAndView mav = new ModelAndView();
		
		 String localeStr = LocaleHelper.getLocaleStr();
		 localeStr=LocaleHelper.localToLangNo(localeStr);

		
		//view
		String viewName = "item/testcategory";
		mav.setViewName(viewName);
		
		//service
		this.itemCategoryService.renameResultSaved(key, newText,localeStr);
		
		//test getDesendants
		List<ItemCategory> list = new ArrayList<ItemCategory>();
		ItemCategory p = this.itemCategoryService.findByCategoryCode(key);		
		list = this.getDesendants(list, p.getCategoryId());
		logger.info("list-size="+list.size());
		for (ItemCategory ic : list) {
			logger.info("category_id="+ic.getCategoryId()+" name="+ic.getCategoryName()+" category_no="+ic.getCategoryCode());
		} 
		//data
		Map<String, Object> model = mav.getModel();
		model.put("old",old);
		model.put("newText", newText); 
		model.put("key", key);
		
		logger.info("Old : " + old + "      New Text : " + newText + "      Key : " + key);
		
		logger.info("leaving /item/renameResultSaved");
//		return mav;
		return model;
	}
	
	@RequestMapping(value="/item/deleteResultSaved",method=RequestMethod.POST,produces="application/json")
	@ResponseBody
	public Map<String, Object> deleteResultSaved(@RequestParam String node){
		logger.info("entering /item/deleteResultSaved");
		
		ModelAndView mav = new ModelAndView();
		
		//view
		String viewName = "item/testcategory";
		mav.setViewName(viewName);
		
		//data
		Map<String, Object> model = mav.getModel();
		
		// Save data to DB
		List<ItemCategory> list = new ArrayList<ItemCategory>();
//		ItemCategory p = this.itemCategoryService.findByCategoryNo(Long.parseLong(parent));
		ItemCategory old = this.itemCategoryService.findByCategoryCode(node);
//		int levelDifference = p.getLevel() - old.getLevel() + 1;
		list.add(old); //getDesendants does not include this node.
		list = this.getDesendants(list, old.getCategoryId());
//		logger.info("p.level="+p.getLevel()+"  old.level="+old.getLevel()+"  levelDifference="+levelDifference);

		for (ItemCategory ic : list) {
			if (ic.getCategoryLevel() != 0) {
				this.itemCategoryService.deleteItemCategoryByCategoryId(ic.getCategoryId());
			}
			
		}
		
//		model.put("parent", parent);
		model.put("node", node);
		
		logger.info("      Deleted Node : " + node);
		
		logger.info("leaving /item/deleteResultSaved");
		return model;
	}

	
	/**
	 * Copy and paste a subtree from one place to another, the keys of the new subtree will be generated accordingly.
	 * the result is persisted into database
	 * @param json subtree to be copied, list of tree_id, text, parent tree_id, parent key
	 * @return all the new generated tree ids as well as the associated value (category code) in a map
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 * @author Fangze Sun
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value="/item/copyAndPatseResultSaved", method=RequestMethod.POST, produces="application/json")
	@ResponseBody
	public Map<String, Object> copyAndPatseResultSaved(@RequestBody final String json) throws JsonParseException, JsonMappingException, IOException{
		logger.info("entering /item/copyAndPatseResultSaved");
		
		ModelAndView mav = new ModelAndView();
		
		//view
		String viewName = "item/testcategory";
		mav.setViewName(viewName);
		
		//data
		Map<String, Object> model = mav.getModel();
		 String localeStr = LocaleHelper.getLocaleStr();
		  localeStr=LocaleHelper.localToLangNo(localeStr);
		HashMap<String, String> newKeys = new HashMap<String, String>();
		
		ObjectMapper mapper = new ObjectMapper();	//JSON to java object mapper
        
        //my JSON variable have the IDs that i need, but i don't know how to get them .
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		list = mapper.readValue(json, List.class);	//type not safe, to check it later
		String id, text, pid, pkey;
		for (Map<String, String> ids : list) {
//			ids = mapper.readValue(json, HashMap.class);
			id 		= ids.get("id");		//tree id of category
	        text 	= ids.get("text");		//category name
	        pid 	= ids.get("pid");		//tree id of parent node
	        pkey 	= ids.get("pkey");		//parent category code
	        if ((pkey == "")) {
	        	pkey = newKeys.get(pid);
	        }
			logger.info("id="+id+" text="+text+" pid="+pid+" pkey="+pkey);
			
			//DB op
			ItemCategory p = this.itemCategoryService.findByCategoryCode(pkey);
	    	long parentId = p.getCategoryId();
	    	int parentLevel = p.getCategoryLevel();
			String newKey = this.itemCategoryService.createResultSaved(parentId, text, parentLevel,localeStr);	//category code
			newKeys.put(id, newKey);
		}
       
		
		model.put("newKeys", newKeys);
		
		logger.info("leaving /item/copyAndPatseResultSaved");
		return model;
	}
	
	@RequestMapping(value="/item/categoryList")
	public String gotoCategoryList(){
		String viewName = "item/item_catetory_list";
		return viewName;
	}
	
	@RequestMapping(value="/item/categoryEdit")
	public String gotoCategoryEdit(){
		String viewName = "item/item_category_edit";
		return viewName;
	}
	
	/**
	 * @param list
	 * @param categoryId
	 * @return
	 * @author Fangze Sun
	 */
	private List<ItemCategory> getDesendants(List<ItemCategory> list, long categoryId) {
//		logger.info("entered getDesendants, categoryId="+categoryId+"  list-size="+list.size());
		List<ItemCategory> tmp = this.itemCategoryService.getChildren(categoryId);
		list.addAll(tmp);
		for (ItemCategory ci : tmp) {
			getDesendants(list, ci.getCategoryId());
		}
		return list;
	}
	@RequestMapping(value="/item/gotoCategoryEdit")
	public ModelAndView gotoCategoryEdit(@RequestParam String categoryId){
		logger.info("entering /item/categoryEdit");
		
		ModelAndView mav = new ModelAndView(); 
		String localeStr = LocaleHelper.getLocaleStr();
	    localeStr=LocaleHelper.localToLangNo(localeStr);
	    
	    
		//view	
		String viewName = "item/item_category_edit";
		mav.setViewName(viewName);
		
		//data
		Map<String, Object> model = mav.getModel();
		
		//data - news
		ItemCategory itemCategory = this.itemCategoryService.findByCategoryId(Long.parseLong(categoryId),localeStr);	 
		System.out.println("categoryObject :"+itemCategory.toString());
		model.put("categoryObject", itemCategory);
		logger.info("leaving /item/categoryEdit");
		return mav;
	} 

	@RequestMapping(value="/item/categoryUpdate",method=RequestMethod.POST)
	@ResponseBody
	public Map<String,Object> categoryUpdate(@RequestParam String itemJSONString){
		logger.info("entering /item/categoryUpdate");
		String localeStr = LocaleHelper.getLocaleStr();
	    localeStr=LocaleHelper.localToLangNo(localeStr);
	    
	    
		System.out.println(itemJSONString);
		
		ModelAndView mav = new ModelAndView();
		JSONObject ic_job= new JSONObject();
		ItemCategory itemCategory = ic_job.parseObject(itemJSONString, ItemCategory.class);
		
		itemCategoryService.updateCategory(itemCategory,localeStr);
		
		logger.info("leaving /item/categoryUpdate");
		Map<String,Object> map=new HashMap<String, Object>();
        map.put("success",true);
        map.put("msg","ok");
		return map;
	}
	
	
	/**
	 * @param listCategory
	 * @return
	 * @author Athens Zhang
	 */
	private String[][] getData(List<ItemCategory> listCategory){
		int entryLength = listCategory.size();
		String localeStr = LocaleHelper.getLocaleStr();
	   // localeStr=LocaleHelper.localToLangNo(localeStr);
		logger.info("localeStr: = "+localeStr);

		logger.info("entryLength: = "+entryLength);
		
		final int COLUMN_NUM = 9;
		String[][] data = new String[entryLength][COLUMN_NUM];
		  
		String field0 = "";	//check box 
		String field1 = "";	//category id
		String field2 = "";	//parent id
		String field3 = "";	//category code
		String field4 = "";	//category name
		String field5 = "";	//category desc
		String field6 = "";	//category level 
		String field7 = "";	//event status
		String field8 = "";	//action
		
		for(int i=0; i<entryLength ; i++){			
			field0 = "<input type='checkbox' name='id[]' value="+listCategory.get(i).getCategoryId()+">";
			field1 = listCategory.get(i).getCategoryId()+"";
			field2 = listCategory.get(i).getParentId()+"";
			field3 = listCategory.get(i).getCategoryCode();
			field4 = listCategory.get(i).getCategoryName();
			field5 = listCategory.get(i).getCategoryDesc();
			field6 = listCategory.get(i).getCategoryLevel()+"";
			
			int intCategoryStatus = listCategory.get(i).getCategoryStatus();
			String[] categoryStatusPair = getCategoryStatusPair(intCategoryStatus);
			String categoryStatusKey = categoryStatusPair[0];  
			String categoryStatus = categoryStatusPair[1];
			field7 = "<span class='label label-sm label-"+categoryStatusKey+"'>"+categoryStatus+"</span>";
//			field8 = "<a href='/acp/item/"+getAction(actionName)+"?eventUUID="+field1+"' class='btn btn-xs default btn-editable'><i class='fa fa-pencil'></i> "+actionName+"</a>";
			field8 = "<a href='/acp/item/gotoCategoryEdit?categoryId="+listCategory.get(i).getCategoryId()+"&lang="+localeStr+"' class='btn btn-xs default btn-editable'><i class='fa fa-pencil'></i>Update</a><button onclick='deleteCategory("+listCategory.get(i).getCategoryId()+")' class='btn btn-xs default btn-editable'><i class='fa fa-pencil'></i> "+"Delete"+"</button>";

			
			//logger.info("field8="+field8);
			
			data[i][0] = field0;
			data[i][1] = field1;
			data[i][2] = field2;
			data[i][3] = field3;
			data[i][4] = field4;
			data[i][5] = field5;
			data[i][6] = field6;
			data[i][7] = field7;
			data[i][8] = field8;
		}
		  
		System.out.println(">>>>>>>>>>");
		System.out.println(">>>>>>>>>> data size = "+data.length);
		//ArrayHelper.printArray(data);		
		
		System.out.println(">>>>>>>>>>");
		data = ManyNodeTree.getPreOrderTreeAsArray(data);
		//ArrayHelper.printArray(data);
		
		return data;
	}
	
	/**
	 * @param listCategory
	 * @return
	 * @author Athens Zhang
	 */
	private String[][] getDataWithoutTree(List<ItemCategory> listCategory){
		int entryLength = listCategory.size();
		
		logger.info("entryLength: = "+entryLength);
		
		final int COLUMN_NUM = 9;
		String[][] data = new String[entryLength][COLUMN_NUM];
		
		String field0 = "";	//check box
		String field1 = "";	//category id
		String field2 = "";	//parent id
		String field3 = "";	//category code
		String field4 = "";	//category name
		String field5 = "";	//category desc
		String field6 = "";	//category level
		String field7 = "";	//event status
		String field8 = "";	//action
		
		for(int i=0; i<entryLength ; i++){			
			field0 = "<input type='checkbox' name='id[]' value="+listCategory.get(i).getCategoryId()+">";
			field1 = listCategory.get(i).getCategoryId()+"";
			field2 = listCategory.get(i).getParentId()+"";
			field3 = listCategory.get(i).getCategoryCode();
			field4 = listCategory.get(i).getCategoryName();
			field5 = listCategory.get(i).getCategoryDesc();
			field6 = listCategory.get(i).getCategoryLevel()+"";
			
			int intCategoryStatus = listCategory.get(i).getCategoryStatus();
			String[] categoryStatusPair = getCategoryStatusPair(intCategoryStatus);
			String categoryStatusKey = categoryStatusPair[0];
			String categoryStatus = categoryStatusPair[1];
			field7 = "<span class='label label-sm label-"+categoryStatusKey+"'>"+categoryStatus+"</span>";
//			field8 = "<a href='/acp/item/"+getAction(actionName)+"?eventUUID="+field1+"' class='btn btn-xs default btn-editable'><i class='fa fa-pencil'></i> "+actionName+"</a>";
			field8 = "<a href='/acp/item/gotoCategoryEdit?categoryId="+listCategory.get(i).getCategoryId()+"' class='btn btn-xs default btn-editable'><i class='fa fa-pencil'></i>Update</a><button onclick='deleteCategory("+listCategory.get(i).getCategoryId()+")' class='btn btn-xs default btn-editable'><i class='fa fa-pencil'></i> "+"Delete"+"</button>";
			
			//logger.info("field8="+field8);
			 
			data[i][0] = field0;
			data[i][1] = field1; 
			data[i][2] = field2;
			data[i][3] = field3;
			data[i][4] = field4;
			data[i][5] = field5;
			data[i][6] = field6;
			data[i][7] = field7;
			data[i][8] = field8;
		}
		
		System.out.println(">>>>>>>>>>");
		System.out.println(">>>>>>>>>> data size = "+data.length);
		
		return data;
	}
	
	private String[] getCategoryStatusPair(int intCategoryStatus){
		String[] statusPair = new String[2];
		
		String status = "";
		String statusKey = "";
		switch(intCategoryStatus){
			case ItemCategoryStatus.AVAILABLE: 
				status = "Available";
				statusKey = "success";
				break;
			case ItemCategoryStatus.UNAVAILABLE: 
				status = "Unavailable";
				statusKey = "warning";
				break;
			case ItemCategoryStatus.DELETED: 
				status = "Deleted";
				statusKey = "default";
				break;
			case ItemCategoryStatus.DISCONTINUED: 
				status = "Discontinued";
				statusKey = "info";
				break;
			case ItemCategoryStatus.UPCOMIGN: 
				status = "Upcoming";
				statusKey = "danger";
				break;
			default: 
				break;
		}
		
		statusPair[0]=statusKey;
		statusPair[1]=status;
		
		
		return statusPair;
	}
}
