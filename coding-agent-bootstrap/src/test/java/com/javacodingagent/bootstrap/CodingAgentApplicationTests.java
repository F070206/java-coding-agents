package com.javacodingagent.bootstrap;
import com.javacodingagent.core.llm.CodingModel;
import com.javacodingagent.core.llm.DeterministicCodingModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "coding-agent.llm.api-key=disabled-test-key")
@AutoConfigureMockMvc
class CodingAgentApplicationTests {
 @Autowired MockMvc mvc;
 @Autowired CodingModel model;

 @Test void contextLoads() { assertThat(model).isInstanceOf(DeterministicCodingModel.class); }
 @Test void servesFrontend() throws Exception { mvc.perform(get("/")).andExpect(status().isOk()); }
 @Test @WithMockUser void bindsPathVariables() throws Exception {
  mvc.perform(get("/api/repositories/123")).andExpect(status().isOk());
  mvc.perform(get("/api/agent/tasks/123/trace")).andExpect(status().isOk());
 }
}
