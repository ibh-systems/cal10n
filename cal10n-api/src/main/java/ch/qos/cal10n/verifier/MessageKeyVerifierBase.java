package ch.qos.cal10n.verifier;

import ch.qos.cal10n.util.CAL10NResourceBundleFinder;
import ch.qos.cal10n.util.IAnnotationExtractor;
import ch.qos.cal10n.util.MiscUtil;
import static ch.qos.cal10n.verifier.CAL10NError.ErrorType.MISSING_LOCALE_DATA_ANNOTATION;
import static ch.qos.cal10n.verifier.CAL10NError.ErrorType.MISSING_BN_ANNOTATION;


import java.util.*;

/**
 * Abstract class for verifying that for a given an enum type, the keys match those
 * found in the corresponding resource bundles.
 *
 * <p>This class contains the bundle verification logic. Logic for extracting locate and key information
 * should be provided by derived classes.</p>
 *
 * @author: Ceki Gulcu
 * @since 0.8
 */
abstract public class MessageKeyVerifierBase implements IMessageKeyVerifier {

  final String enumTypeAsStr;
  final IAnnotationExtractor annotationExtractor;

  protected MessageKeyVerifierBase(String enumTypeAsStr, IAnnotationExtractor annotationExtractor) {
    this.enumTypeAsStr = enumTypeAsStr;
    this.annotationExtractor = annotationExtractor;
  }

  public String getEnumTypeAsStr() {
    return enumTypeAsStr;
  }

  protected String extractCharsetForLocale(Locale locale) {
    return annotationExtractor.extractCharset(locale);
  }

  abstract protected List<String> extractKeysInEnum();


  public String[] getLocaleNames() {
    String[] localeNameArray = annotationExtractor.extractLocaleNames();
    return localeNameArray;
  }

  public String getBaseName() {
    String rbName = annotationExtractor.getBaseName();
    return rbName;
  }

  public List<CAL10NError> verify(Locale locale) {
    List<CAL10NError> errorList = new ArrayList<CAL10NError>();

    String baseName = getBaseName();

    if (baseName == null) {
      errorList.add(new CAL10NError(MISSING_BN_ANNOTATION, "",
              enumTypeAsStr, locale, ""));
      return errorList;
    }

    String charset = extractCharsetForLocale(locale);

    ResourceBundle rb = CAL10NResourceBundleFinder.getBundle(this.getClass()
            .getClassLoader(), baseName, locale, charset);

    ErrorFactory errorFactory = new ErrorFactory(enumTypeAsStr, locale, baseName);

    if (rb == null) {
      errorList.add(errorFactory.buildError(CAL10NError.ErrorType.FAILED_TO_FIND_RB, ""));
      return errorList;
    }

    Set<String> rbKeySet = buildKeySetFromEnumeration(rb.getKeys());

    if (rbKeySet.size() == 0) {
      errorList.add(errorFactory.buildError(CAL10NError.ErrorType.EMPTY_RB, ""));
    }

    if (errorList.size() != 0) {
      return errorList;
    }

    List<String> enumKeys = extractKeysInEnum();
    if (enumKeys.size() == 0) {
      errorList.add(errorFactory.buildError(CAL10NError.ErrorType.EMPTY_ENUM, ""));
    }

    for (String enumKey : enumKeys) {
      if (rbKeySet.contains(enumKey)) {
        rbKeySet.remove(enumKey);
      } else {
        errorList.add(errorFactory.buildError(CAL10NError.ErrorType.ABSENT_IN_RB, enumKey));
      }
    }

    for (String rbKey : rbKeySet) {
      errorList.add(errorFactory.buildError(CAL10NError.ErrorType.ABSENT_IN_ENUM, rbKey));
    }
    return errorList;
  }

  public List<String> typeIsolatedVerify(Locale locale) {
    List<CAL10NError> errorList = verify(locale);
    List<String> strList = new ArrayList<String>();
    for (CAL10NError error : errorList) {
      strList.add(error.toString());
    }
    return strList;
  }

  protected Set<String> buildKeySetFromEnumeration(Enumeration<String> e) {
    Set<String> set = new HashSet<String>();
    while (e.hasMoreElements()) {
      String s = e.nextElement();
      set.add(s);
    }
    return set;
  }
  /**
   * Verify all declared locales in one step.
   */
  public List<CAL10NError> verifyAllLocales() {
    List<CAL10NError> errorList = new ArrayList<CAL10NError>();

    String[] localeNameArray = getLocaleNames();

    ErrorFactory errorFactory = new ErrorFactory(enumTypeAsStr, null,  getBaseName());


    if (localeNameArray == null || localeNameArray.length == 0) {
      errorList.add(errorFactory.buildError(MISSING_LOCALE_DATA_ANNOTATION, "*"));
      return errorList;
    }
    for (String localeName : localeNameArray) {
      Locale locale = MiscUtil.toLocale(localeName);
      List<CAL10NError> tmpList = verify(locale);
      errorList.addAll(tmpList);
    }

    return errorList;
  }



}
