package com.eco.bio7.javaeditors;

import java.util.ArrayList;
import java.util.List;

class MainClass {
		private String name;
		private int sort;
		private List<ClassMembers> todos = new ArrayList<ClassMembers>();
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public int getSort() {
			return sort;
		}
		public void setSort(int sort) {
			this.sort = sort;
		}
		
		public List<ClassMembers> getTodos (){
			return todos;
		}
		
		
	}