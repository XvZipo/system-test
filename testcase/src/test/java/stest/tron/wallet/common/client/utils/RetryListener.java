package stest.tron.wallet.common.client.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.testng.IAnnotationTransformer;
import org.testng.IRetryAnalyzer;
import org.testng.annotations.ITestAnnotation;

public class RetryListener implements IAnnotationTransformer {

  @Override
  public void transform(ITestAnnotation testannotation, Class testClass,
      Constructor testConstructor, Method testMethod) {
    Class<? extends IRetryAnalyzer> retryClass = testannotation.getRetryAnalyzerClass();

    if (retryClass == null) {
      testannotation.setRetryAnalyzer(Retry.class);
    }
  }
}
