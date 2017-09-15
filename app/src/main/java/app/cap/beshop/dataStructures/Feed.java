package app.cap.beshop.dataStructures;

public class Feed {

    private String desc, image, title, u_id, date, delete;

    public Feed() {
    }


    public Feed(String desc, String image, String title, String date, String delete) {
        this.desc = desc;
        this.image = image;
        this.title = title;
        this.date = date;
        this.delete = delete;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getU_id() {
        return u_id;
    }

    public void setU_id(String u_id) {
        this.u_id = u_id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDelete(){return delete;}

    public void setDelete(String delete){this.delete = delete;}
}
