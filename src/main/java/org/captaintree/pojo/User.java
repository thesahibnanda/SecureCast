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
}
