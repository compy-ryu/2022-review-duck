import { useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';

import cn from 'classnames';

import { TemplateFilterType } from 'service/@shared/types';

import { useGetTemplates } from 'service/@shared/hooks/queries/template/useGet';

import { Button } from 'common/components';

import Icon from 'common/components/Icon';

import LayoutContainer from 'service/@shared/components/LayoutContainer';

import styles from './styles.module.scss';

import TemplateListContainer from './TemplateListContainer';
import { PAGE_LIST, TEMPLATE_TAB } from 'service/@shared/constants';

function TemplateListPage() {
  const navigate = useNavigate();
  const [searchParam] = useSearchParams();

  const currentTab = searchParam.get('filter') as TemplateFilterType;

  const { data, isError, error } = useGetTemplates(currentTab);

  const { templates } = data || { templates: [] };

  useEffect(() => {
    if (isError) {
      alert(error?.message);
      navigate(-1);
    }
  }, [isError, error]);

  return (
    <LayoutContainer>
      <div>
        <div className={styles.header}>
          <div>
            <Link to={`${PAGE_LIST.TEMPLATE_LIST}?filter=${TEMPLATE_TAB.TREND}`}>
              <button className={styles.button}>
                <div
                  className={cn(styles.buttonBox, {
                    [styles.focus]: currentTab === TEMPLATE_TAB.TREND,
                  })}
                >
                  <Icon className={styles.icon} code="local_fire_department" />
                  트랜딩
                </div>
              </button>
            </Link>
            <Link to={`${PAGE_LIST.TEMPLATE_LIST}?filter=${TEMPLATE_TAB.LATEST}`}>
              <button className={styles.button}>
                <div
                  className={cn(styles.buttonBox, {
                    [styles.focus]: currentTab === TEMPLATE_TAB.LATEST,
                  })}
                >
                  <Icon className={styles.icon} code="playlist_add_check_circle" />
                  최신
                </div>
              </button>
            </Link>
          </div>
          <div className={styles.buttonContainer}>
            <Link to={PAGE_LIST.TEMPLATE_FORM}>
              <Button>
                <Icon code="rate_review" />
                템플릿 만들기
              </Button>
            </Link>
          </div>
        </div>
        <TemplateListContainer templates={templates} />
      </div>
    </LayoutContainer>
  );
}

export default TemplateListPage;