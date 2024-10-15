package com.surendramaran.yolov8tflite.Entidades;

public class Usuario {

    String idUser;
    String user;
    String password;

    /*public Usuario( String idUser, String user, String password) {
        this.idUser = idUser;
        this.user = user;
        this.password = password;
    }*/

    public String getIdUser() {
        return idUser;
    }

    public void setIdUser(String idUser) {
        this.idUser = idUser;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "idUser='" + idUser + '\'' +
                ", user='" + user + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
