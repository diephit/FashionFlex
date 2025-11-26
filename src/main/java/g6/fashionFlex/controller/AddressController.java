package g6.fashionFlex.controller;

import g6.fashionFlex.dto.AddressDTO;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.service.AddressService;
import g6.fashionFlex.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/user/address")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String viewAddressBook(Model model, @AuthenticationPrincipal(expression = "user") User currentUser) {
        List<AddressDTO> addresses = addressService.findByUserId(currentUser.getId());
        model.addAttribute("addresses", addresses);
        model.addAttribute("user", currentUser);
        return "user/address-book";
    }

    @GetMapping("/add")
    public String showAddAddressForm(Model model, @AuthenticationPrincipal(expression = "user") User currentUser) {
        model.addAttribute("address", new AddressDTO());
        model.addAttribute("user", currentUser);
        return "user/address-form";
    }

    @PostMapping("/add")
    public String addAddress(@ModelAttribute("address") AddressDTO addressDTO, @AuthenticationPrincipal(expression = "user") User currentUser) {
        addressService.save(addressDTO, currentUser.getId());
        return "redirect:/user/address";
    }

    @GetMapping("/edit/{id}")
    public String showEditAddressForm(@PathVariable("id") Long id, Model model) {
        AddressDTO addressDTO = addressService.findById(id);
        model.addAttribute("address", addressDTO);
        return "user/address-form";
    }

    @PostMapping("/edit/{id}")
    public String editAddress(@PathVariable("id") Long id, @ModelAttribute("address") AddressDTO addressDTO, @AuthenticationPrincipal(expression = "user") User currentUser) {
        addressDTO.setId(id);
        addressService.save(addressDTO, currentUser.getId());
        return "redirect:/user/address";
    }

    @GetMapping("/delete/{id}")
    public String deleteAddress(@PathVariable("id") Long id) {
        addressService.deleteById(id);
        return "redirect:/user/address";
    }

    @GetMapping("/set-default/{id}")
    public String setDefaultAddress(@PathVariable("id") Long id, @AuthenticationPrincipal(expression = "user") User currentUser) {
        addressService.setDefault(id, currentUser.getId());
        return "redirect:/user/address";
    }
}
