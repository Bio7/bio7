package com.eco.bio7.floweditor.model;



import org.eclipse.gef.requests.CreationFactory;

class FlowModelFactory implements CreationFactory {
	private String path; 
	public Object getNewObject() {
		BeanShellScript model = new BeanShellScript();
	
		model.setText(path);
		String ext = path;
        String s = path;
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
		model.setTextFiletype(ext);
		return model;
	}

	public Object getObjectType() {
		return BeanShellScript.class;
	}

	
	public void setPath(String s) {
		path = s;
	}
}
