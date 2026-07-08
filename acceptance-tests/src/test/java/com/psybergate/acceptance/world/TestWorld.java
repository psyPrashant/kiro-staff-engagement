package com.psybergate.acceptance.world;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ScenarioScope
public class TestWorld {

	private final Map<String, Object> state = new HashMap<>();

	public <T> void set(String key, T value) {
		state.put(key, value);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		return (T) state.get(key);
	}

	public void clear() {
		state.clear();
	}
}
