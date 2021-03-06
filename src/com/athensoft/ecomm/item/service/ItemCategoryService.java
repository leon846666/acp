package com.athensoft.ecomm.item.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athensoft.ecomm.item.dao.ItemCategoryDao;
import com.athensoft.ecomm.item.dao.ItemCategoryI18nDao;
import com.athensoft.ecomm.item.entity.ItemCategory;

/**
 * @author Athens
 *
 */
@Service
@Transactional(rollbackFor=Exception.class)
public class ItemCategoryService {
	
	@Autowired
	@Qualifier("itemCategoryDaoJDBCImpl")
	private ItemCategoryDao itemCategoryDao;

	@Autowired
	@Qualifier("itemCategoryI18nDaoJDBCImpl")
	private ItemCategoryI18nDao itemCategoryI18nDao;
	
	

	public void setItemCategoryDao(ItemCategoryDao itemCategoryDao) {
		this.itemCategoryDao = itemCategoryDao;
	}

	public void dragAndDropResultSaved(String orig, String dest) {
		this.itemCategoryDao.dragAndDropResultSaved(orig, dest);
	}

	public void renameResultSaved(String key, String newText,String localeStr) {
		this.itemCategoryDao.renameResultSaved(key, newText,localeStr);
	}

	public List<ItemCategory> findAll(String localeStr) {
		return this.itemCategoryDao.findAll(localeStr);
	}
	
	public ItemCategory findByCategoryId(long categoryId,String localeStr) {
		return this.itemCategoryDao.findByCategoryId(categoryId,localeStr);
	}
	
	//findTreeByCategoryId
	public List<ItemCategory> findCategoryTreeByCategoryId(String localeStr,int categoryId) { 
		
		return this.itemCategoryDao.findCategoryTreeByCategoryId(localeStr,categoryId);
	}

	public ItemCategory findByCategoryCode(String categoryCode) {
		return this.itemCategoryDao.findByCategoryCode(categoryCode);
	}
	
	
	/**
	 * @return
	 * @author Athens
	 */
	public List<ItemCategory> getCategoryByFilter(String queryString,String localeStr){
		return this.itemCategoryDao.findByFilter(queryString,localeStr);
	}

	public String createResultSaved(long parentId, String text, int parentLevel,String localeStr) {
		String categoryCode=this.itemCategoryDao.getCategoryCodeByParentId(parentId);
		ItemCategory itemCategory = new ItemCategory();
		itemCategory.setCategoryCode(categoryCode);
		itemCategory.setCategoryName(text);
		itemCategory.setParentId(parentId);
		int createCategory = this.createCategory(itemCategory,localeStr);
		
		return "";
	}

	public List<ItemCategory> getChildren(long categoryId) {
		return this.itemCategoryDao.getChildren(categoryId);
	}

	public void updateItemCategoryParent(long categoryId, long parentId, int level) {
		this.itemCategoryDao.updateItemCategoryParent(categoryId, parentId, level);
	}

	public int deleteItemCategoryByCategoryId(long categoryId) {
		int result1 = this.itemCategoryI18nDao.deleteCategoryI18nByCategoryId(categoryId);
		int result2= this.itemCategoryDao.deleteItemCategoryByCategoryId(categoryId);
		
		if(result1==1 && result2==1){
			return 1;
		}else{
			return 0;
		}
		
	}

	public List<ItemCategory> findAllParentCategories() {
		// TODO Auto-generated method stub
		return this.itemCategoryDao.findAllParentCategories();
	}
	
	/**
	 * function to create category, generate category code in different cases
	 * 
	 * @author Yang Liu
	 *
	 */

	public int createCategory(ItemCategory itemCategory,String localeStr) {
		// TODO Auto-generated method stub
		String newStringCateCode="";
		int categoryLevel=0;
		int newIntCategoryCode=0;
		String newCategoryCode="";

		if(itemCategory.getParentId()==null){
			ItemCategory parentItemCatgory = this.itemCategoryDao.findByCategoryCode(itemCategory.getCategoryCode());
			itemCategory.setParentId(parentItemCatgory.getCategoryId());
		}
		 
		
		if(!itemCategory.getCategoryCode().equals("ROOT")){
			categoryLevel=itemCategory.getCategoryCode().split("-").length;
		}
		
		int  childLevel = categoryLevel+1;
		
		boolean isParent=this.itemCategoryDao.ifisParent(itemCategory.getCategoryCode(),childLevel);
		if(itemCategory.getCategoryCode().equals("ROOT")){
			isParent=true;
		}
		 
		if(isParent){
			 
			if("ROOT".equals(itemCategory.getCategoryCode())){
				newCategoryCode = this.itemCategoryDao.getInsertedCateCode(childLevel);
				newIntCategoryCode= Integer.parseInt(newCategoryCode.split("-")[0]);
				newIntCategoryCode++;
				 
				if(newIntCategoryCode<10){
					newStringCateCode="00"+newIntCategoryCode;
				}
				else if(newIntCategoryCode<=100&&newIntCategoryCode>10){
					newStringCateCode=""+newIntCategoryCode;
 
				}
			}else{
			    newCategoryCode = this.itemCategoryDao.getInsertedCateCode(itemCategory.getCategoryCode(),childLevel);

				int length=newCategoryCode.split("-").length;
				System.out.println(length);
				if(length>1){
					newIntCategoryCode= Integer.parseInt(newCategoryCode.split("-")[newCategoryCode.split("-").length-1]);
				}
				newIntCategoryCode++; 
				if(newIntCategoryCode<10){
					newCategoryCode.substring(newCategoryCode.length()-3);
					newStringCateCode=newCategoryCode.substring(0, newCategoryCode.lastIndexOf(newCategoryCode.substring(newCategoryCode.length()-3)))+"00"+newIntCategoryCode;
				}
				else if(newIntCategoryCode<=100&&newIntCategoryCode>10){
					newStringCateCode=newCategoryCode.substring(0, newCategoryCode.lastIndexOf(newCategoryCode.substring(newCategoryCode.length()-3)))+"0"+newIntCategoryCode;
				}
			}
			
		}
		else{
			newStringCateCode=itemCategory.getCategoryCode()+"-001";

		}
		
		itemCategory.setCategoryCode(newStringCateCode);
		itemCategory.setCategoryLevel(childLevel); 
		
		int result1 = this.itemCategoryDao.createCategory(itemCategory);
		int result2 =0;
		if(localeStr!=null){
			result2= this.itemCategoryI18nDao.createCategoryI18n(itemCategory,result1,localeStr);
		}else{
			 result2 = this.itemCategoryI18nDao.createCategoryI18n(itemCategory,result1);
			
		}
		
		if(result1==1&&result2==1){
			return 1;
		}else{
			return 2;
		}
		
		
	}

	public void updateCategory(ItemCategory itemCategory,String localeStr) {
		ItemCategory parentItemCatgory=new ItemCategory();
		if(itemCategory.getCategoryCode().split("-").length!=1){ 
			String parentCode= itemCategory.getCategoryCode().substring(0,itemCategory.getCategoryCode().length()-4);
			System.out.println("parentCode"+ parentCode);
			parentItemCatgory	 = this.itemCategoryDao.findByCategoryCode(parentCode);
			itemCategory.setParentId(parentItemCatgory.getCategoryId());
		}else{
			itemCategory.setParentId(1l);
		}
		int categoryLevel=parentItemCatgory.getCategoryCode().split("-").length+1;
	
		itemCategory.setCategoryLevel(categoryLevel);
		
		  
		  
		this.itemCategoryDao.updateCategory(itemCategory,localeStr);
	}

	public void batchUpdateCategory(ArrayList<ItemCategory> cateList) {
		// TODO Auto-generated method stub
		itemCategoryDao.batchUpdateCategory(cateList);
		
	} 

}
