package app.cap.beshop;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import me.drakeet.multitype.Items;
import me.drakeet.support.about.AbsAboutActivity;
import me.drakeet.support.about.BuildConfig;
import me.drakeet.support.about.Card;
import me.drakeet.support.about.Category;
import me.drakeet.support.about.Contributor;
import me.drakeet.support.about.License;
import me.drakeet.support.about.Line;
import me.drakeet.support.about.R;

public class AboutActivity extends AbsAboutActivity {
    @Override
    @SuppressLint("SetTextI18n")
    protected void onCreateHeader(ImageView icon, TextView slogan, TextView version) {
        icon.setImageResource(R.drawable.logo1);
        slogan.setText("Betriever");
        version.setText("v" + BuildConfig.VERSION_NAME);
    }
        @Override @SuppressWarnings("SpellCheckingInspection")
        protected void onItemsCreated (@NonNull Items items){
        /* @formatter:off */
            items.add(new Category("소개"));
            items.add(new Card(getString(R.string.about_page), "공유"));
            items.add(new Line());
            items.add(new Category("도움말"));
            items.add(new Contributor(R.drawable.ic_filter_list_black_24dp, "상품 정보 및 결제 서비스", "상품리스트에서 상품을 선택하여 결제하기를 클릭하면 BLE 통신을 통해 POS 디바이스를 찾게됩니다." +
                    "페어링 절차를 거치지 않으며, 만약 일정 시간 동안 POS 디바이스를 검색하지 못하면 결제가 진행되지 않습니다."));
            items.add(new Contributor(R.drawable.ic_home_black_24dp, "비콘 서비스", "앱을 설치하고 실행하면 바로 주변의 비콘을 찾기 시작합니다. " +
                    "주변에 검색 된 비콘을 통해 사용자와 비콘간의 거리를 계산하여 사용자가 비콘으로 인식 가능한 충분히 가까운 거리에 있는지를 검색합니다."));
            items.add(new Contributor(R.drawable.ic_question_answer_black_24dp, "오픈채팅 기능","비콘과 연동이 가능한 상태가 되면 사용자는 랜덤으로 아이디를 부여받고, " +
                    "주변 사용자들과 오픈 채팅 서비스를 진행할 수 있습니다."));
            items.add(new Contributor(R.drawable.ic_view_module_black_24dp, "사진 업로드", "사용자가 상품 사진을 업로드하면 메인 화면에 업로드 되며, " +
                    "센스 있는 제목과 한줄 평을 통해 좋아요를 받아보세요!!"));

            items.add(new Category("주의 사항"));
            items.add(new Contributor(R.drawable.logo1, "비트리버팀", "이 앱은 비콘을 이용한 O2O서비스의 적용을 목적으로 하는 학술적 용도의 앱입니다." +
                    "즐거운 앱 사용 부탁드릴게요:)\n Apache license 2.0 적용"));
            items.add(new Line());

            items.add(new Category("오픈 소스 라이센스"));
            items.add(new License("MultiTpye", "drakeet", License.APACHE_2, "https://github.com/drakeet/MultiType"));
            items.add(new License("about-page", "drakeet", License.APACHE_2, "https://github.com/drakeet/about-page"));
            items.add(new License("Point of Sales", "doniwinta",License.APACHE_2, "https://github.com/doniwinata/Point-of-sales-BLE-Payment"));
        }


        @Override
        protected void onActionClick (View action){
            onClickShare();
        }
        public void onClickShare() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Betriever");
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.about_page));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(Intent.createChooser(intent, getTitle()));
    }
}

