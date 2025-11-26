package g6.fashionFlex.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private boolean enabled;
    private String provider;
    private boolean hasSetPassword;
    private String profileImageUrl;
    private Set<String> roles;
    private LocalDateTime createdAt;
}
