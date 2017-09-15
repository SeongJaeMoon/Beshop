package app.cap.beshop.dataStructures;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class Constant {
    public static final List<Integer> QUANTITY_LIST = new ArrayList<Integer>();

    static {
        for (int i = 1; i < 11; i++) QUANTITY_LIST.add(i);
    }

    public static final Product PRODUCT1 = new Product(1, "갤럭시 S6", BigDecimal.valueOf(20), "갤럭시 S6 말이 필요 없습니다.", "samsung_galaxy_s6");
    public static final Product PRODUCT2 = new Product(2, "M8", BigDecimal.valueOf(10), "적당한 성능과 20시간 이상의 배터리 이용률이 장점입니다.", "htc_one_m8");
    public static final Product PRODUCT3 = new Product(3, "샤오미", BigDecimal.valueOf(5), "가성비가 좋은 핸드폰입니다...", "xiaomi_mi3");

    public static final List<Product> PRODUCT_LIST = new ArrayList<Product>();

    static {
        PRODUCT_LIST.add(PRODUCT1);
        PRODUCT_LIST.add(PRODUCT2);
        PRODUCT_LIST.add(PRODUCT3);
    }

    public static final String CURRENCY = "만원";
}
