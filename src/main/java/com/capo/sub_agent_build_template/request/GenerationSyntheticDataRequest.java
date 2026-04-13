package com.capo.sub_agent_build_template.request;

import java.util.List;

public class GenerationSyntheticDataRequest {
	
	private String prompt;
	private List<String> imageReferences;

	public String getPrompt() {
		return prompt;
	}

	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}

	public List<String> getImageReferences() {
		return imageReferences;
	}

	public void setImageReferences(List<String> imageReferences) {
		this.imageReferences = imageReferences;
	}
	
	
}
