package plan4life.use_case.set_preferences;

import plan4life.data_access.UserPreferencesDataAccessInterface;
import plan4life.entities.UserPreferences;

public class SetPreferencesInteractor implements SetPreferencesInputBoundary {

    final SetPreferencesOutputBoundary presenter;
    final UserPreferencesDataAccessInterface dataAccess;

    public SetPreferencesInteractor(SetPreferencesOutputBoundary presenter,
                                    UserPreferencesDataAccessInterface dataAccess) {
        this.presenter = presenter;
        this.dataAccess = dataAccess;
    }

    @Override
    public void execute(SetPreferencesRequestModel requestModel) {
        try {
            // 1. 创建实体
            UserPreferences preferences = new UserPreferences(
                    requestModel.getTheme(),
                    requestModel.getLanguage(),
                    requestModel.getDefaultReminderMinutes(),
                    requestModel.getTimeZoneId()
            );

            // 2. 保存数据
            dataAccess.save(preferences);

            // 3. 告诉 Presenter 成功了
            // (这里可以创建一个 ResponseModel，但为了简单我们直接传字符串或 null)
            // 假设你的 OutputBoundary 定义了 prepareSuccessView 需要参数，这里简化处理：
            // 你需要去 SetPreferencesOutputBoundary.java 确认方法签名。
            // 假设我们不需要回传复杂数据：
            presenter.prepareSuccessView(null);

        } catch (Exception e) {
            presenter.prepareFailView(e.getMessage());
        }
    }
}