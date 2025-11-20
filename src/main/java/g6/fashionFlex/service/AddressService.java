package g6.fashionFlex.service;

import g6.fashionFlex.dto.AddressDTO;

import java.util.List;

public interface AddressService {
    List<AddressDTO> findByUserId(Long userId);
    AddressDTO findById(Long id);
    AddressDTO save(AddressDTO addressDTO, Long userId);
    void deleteById(Long id);
    void setDefault(Long addressId, Long userId);
    long getUserAddressCount(Long userId);
}