package com.example.jsppractice.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.jsppractice.dto.DeliveryFileImportOutcome;
import com.example.jsppractice.dto.DeliveryImportResult;
import com.example.jsppractice.exception.SftpOperationException;
import com.example.jsppractice.service.DeliveryImportScheduler;

@RunWith(MockitoJUnitRunner.class)
public class DeliveryImportControllerTest {

	@Mock
	private DeliveryImportScheduler deliveryImportScheduler;

	@InjectMocks
	private DeliveryImportController deliveryImportController;

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(deliveryImportController).build();
	}

	@Test
	public void showRendersPage() throws Exception {
		mockMvc.perform(get("/deliveries")).andExpect(status().isOk()).andExpect(view().name("deliveries/show"));
	}

	@Test
	public void runRedirectsWithOutcomesAndSummaryMessage() throws Exception {
		DeliveryImportResult result = new DeliveryImportResult(2, List.of());
		List<DeliveryFileImportOutcome> outcomes = List.of(new DeliveryFileImportOutcome("file.txt", result, null));
		when(deliveryImportScheduler.importPendingDeliveries()).thenReturn(outcomes);

		mockMvc.perform(post("/deliveries/run"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/deliveries"))
				.andExpect(flash().attribute("outcomes", outcomes))
				.andExpect(flash().attribute("flashMessage", "已處理 1 個檔案"));
	}

	@Test
	public void runShowsErrorMessageWhenSftpConnectionFails() throws Exception {
		when(deliveryImportScheduler.importPendingDeliveries())
				.thenThrow(new SftpOperationException("SFTP 操作失敗：連線逾時", null));

		mockMvc.perform(post("/deliveries/run"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/deliveries"))
				.andExpect(flash().attribute("flashMessage", "SFTP 操作失敗：連線逾時"));
	}
}
