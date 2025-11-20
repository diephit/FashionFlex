package g6.fashionFlex.service.impl;

import g6.fashionFlex.dto.AddressDTO;
import g6.fashionFlex.entity.Address;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.repository.AddressRepository;
import g6.fashionFlex.repository.UserRepository;
import g6.fashionFlex.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<AddressDTO> findByUserId(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AddressDTO findById(Long id) {
        Address address = addressRepository.findById(id).orElse(null);
        return toDTO(address);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AddressDTO save(AddressDTO addressDTO, Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }
        Address address = toEntity(addressDTO);
        address.setUser(user);
        if (address.isDefault()) {
            setExistingDefaultsToFalse(userId);
        }
        return toDTO(addressRepository.save(address));
    }

    @Override
    public void deleteById(Long id) {
        addressRepository.deleteById(id);
    }

    @Override
    public void setDefault(Long addressId, Long userId) {
        setExistingDefaultsToFalse(userId);
        Address address = addressRepository.findById(addressId).orElse(null);
        if (address != null && address.getUser().getId().equals(userId)) {
            address.setDefault(true);
            addressRepository.save(address);
        }
    }

    private void setExistingDefaultsToFalse(Long userId) {
        List<Address> addresses = addressRepository.findByUserId(userId);
        for (Address addr : addresses) {
            if (addr.isDefault()) {
                addr.setDefault(false);
                addressRepository.save(addr);
            }
        }
    }

    private AddressDTO toDTO(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressDTO(
                address.getId(),
                address.getFullName(),
                address.getPhoneNumber(),
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getZipCode(),
                address.getCountry(),
                address.isDefault()
        );
    }

    private Address toEntity(AddressDTO addressDTO) {
        if (addressDTO == null) {
            return null;
        }
        Address address = new Address();
        address.setId(addressDTO.getId());
        address.setFullName(addressDTO.getFullName());
        address.setPhoneNumber(addressDTO.getPhoneNumber());
        address.setStreet(addressDTO.getStreet());
        address.setCity(addressDTO.getCity());
        address.setState(addressDTO.getState());
        address.setZipCode(addressDTO.getZipCode());
        address.setCountry(addressDTO.getCountry());
        address.setDefault(addressDTO.isDefault());
        return address;
    }

    @Override
    public long getUserAddressCount(Long userId) {
        return addressRepository.countByUserId(userId);
    }
}