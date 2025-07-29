/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.vet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

/**
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
@Controller
class VetController {

	private final RestTemplate restTemplate;

	@Value("${petclinic.vet.service.url}")
	private String vetServiceUrl;

	public VetController(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@GetMapping("/vets.html")
	public String showVetList(@RequestParam(defaultValue = "1") int page, Model model) {
		String url = this.vetServiceUrl + "/vets";
		Vet[] vetsResponse = restTemplate.getForObject(url, Vet[].class);
		List<Vet> allVets = Arrays.asList(vetsResponse);

		for (Vet vet : allVets) {
			System.out.println("Vet: " + vet.getFirstName());
		}

		int pageSize = 5;
		Page<Vet> paginated = paginateList(allVets, page, pageSize);

		return addPaginationModel(page, paginated, model);
	}

	private Page<Vet> paginateList(List<Vet> allVets, int page, int pageSize) {
		int start = Math.min((page - 1) * pageSize, allVets.size());
		int end = Math.min(start + pageSize, allVets.size());
		List<Vet> paginatedList = allVets.subList(start, end);
		return new PageImpl<>(paginatedList, PageRequest.of(page - 1, pageSize), allVets.size());
	}

	private String addPaginationModel(int page, Page<Vet> paginated, Model model) {
		List<Vet> listVets = paginated.getContent();
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", paginated.getTotalPages());
		model.addAttribute("totalItems", paginated.getTotalElements());
		model.addAttribute("listVets", listVets);
		return "vets/vetList";
	}

	// @GetMapping({ "/vets" })
	// public @ResponseBody Vets showResourcesVetList() {
	// // Here we are returning an object of type 'Vets' rather than a collection of Vet
	// // objects so it is simpler for JSon/Object mapping
	// Vets vets = new Vets();
	// vets.getVetList().addAll(this.vetRepository.findAll());
	// return vets;
	// }

}
