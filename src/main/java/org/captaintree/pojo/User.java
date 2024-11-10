package org.captaintree.pojo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class User {
    private String name;
    private String email;
    private String address;
    private String faceId;
    private String aadharCardNumber;

    public static User getGenesisUser() {
        User genesisUser = new User();
        genesisUser.setName("Genesis User");
        genesisUser.setEmail("genesis@example.com");
        genesisUser.setAddress("1234 Genesis Street, Origin City");
        genesisUser.setFaceId("GENESIS_FACE_ID");
        genesisUser.setAadharCardNumber("0000-1111-2222");
        return genesisUser;
    }
}

