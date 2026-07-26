package com.javacodingagent.infrastructure.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javacodingagent.core.llm.CodingModel;
import com.javacodingagent.core.model.*;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.time.Duration;
import java.util.List;

/** Real OpenAI-compatible adapter. Prompts request JSON and responses are mapped to domain records. */
public final class OpenAiCompatibleCodingModel implements CodingModel {
 private final OpenAiChatModel model; private final ObjectMapper mapper;
 public OpenAiCompatibleCodingModel(String baseUrl, String apiKey, String modelName, ObjectMapper mapper) {
  this.model = OpenAiChatModel.builder().baseUrl(baseUrl).apiKey(apiKey).modelName(modelName).temperature(0.1).timeout(Duration.ofSeconds(90)).maxRetries(2).build(); this.mapper = mapper;
 }
 public TaskPlan plan(String requirement, List<String> context) { return decode(model.chat("Return only JSON matching TaskPlan fields goal,targetModules,symbolsToRetrieve,filesToRead,expectedChanges,testStrategy,risks. Requirement: " + requirement + "\nRetrieved context:\n" + String.join("\n", context)), TaskPlan.class); }
 public ChangeProposal proposeChanges(TaskPlan plan, List<String> context, StructuredError previousError) { return decode(model.chat("Return only JSON matching ChangeProposal fields explanation,requiresConfirmation,edits. Each edit has operation CREATE or PATCH,filePath,expected,replacement,content. Use PATCH for existing files. Plan: " + plan + "\nPrevious error: " + previousError + "\nContext:\n" + String.join("\n", context)), ChangeProposal.class); }
 public StructuredError analyzeError(String output) { return decode(model.chat("Return only JSON matching StructuredError fields errorType,filePath,lineNumber,symbol,probableCause,recommendedFix,autoRepairAllowed. Maven output:\n" + output), StructuredError.class); }
 private <T> T decode(String json, Class<T> type) { try { int start = json.indexOf('{'), end = json.lastIndexOf('}'); if (start < 0 || end < start) throw new IllegalArgumentException("Model did not return JSON"); return mapper.readValue(json.substring(start, end + 1), type); } catch (Exception e) { throw new IllegalStateException("MODEL_DECISION_FAILED", e); } }
}
