import { useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import cn from 'classnames';

import useSnackbar from 'common/hooks/useSnackbar';

import { getElapsedTimeText } from 'service/@shared/utils';

import { Button, Icon, Text } from 'common/components';

import ScrollPanel from 'common/components/ScrollPanel';
import TagLabel from 'common/components/TagLabel';

import LayoutContainer from 'service/@shared/components/LayoutContainer';
import Questions from 'service/@shared/components/Questions';
import SmallProfileCard from 'service/@shared/components/SmallProfileCard';
import TemplateCard from 'service/template/components/TemplateCard';

import GithubIcon from 'assets/images/github.svg';

import styles from './styles.module.scss';

import useTemplateDetailQueries from './useTemplateDetailQueries';
import { GITHUB_PROFILE_URL, PAGE_LIST, TEMPLATE_TAB } from 'service/@shared/constants';

function TemplateDetailPage() {
  const { templateId } = useParams();
  const navigate = useNavigate();
  const { showSnackbar } = useSnackbar();

  const { template, templates, isLoadError, loadError, createFormMutation, deleteMutation } =
    useTemplateDetailQueries(Number(templateId));

  const { templates: trendingTemplates } = templates || { templates: [] };

  useEffect(() => {
    if (isLoadError) {
      alert(loadError?.message);
      navigate(-1);
    }
  }, [isLoadError, loadError]);

  const handleDeleteTemplate = (templateId: number) => () => {
    if (confirm('정말 템플릿을 삭제하시겠습니까?\n취소 후 복구를 할 수 없습니다.')) {
      deleteMutation.mutate(templateId, {
        onSuccess: () => {
          showSnackbar({
            title: '템플릿이 삭제되었습니다.',
            description: '사람들과 공유할 새로운 템플릿을 만들어보세요.',
          });
          navigate(-1);
        },
        onError: ({ message }) => {
          alert(message);
        },
      });
    }
  };

  const handleCreateSuccess = ({ reviewFormCode }: Record<'reviewFormCode', string>) => {
    showSnackbar({
      title: '템플릿을 이용해 회고를 생성했습니다.',
      description: '답변을 바로 작성하고 팀원과 공유할 수 있습니다.',
    });

    navigate(`${PAGE_LIST.REVIEW}/${reviewFormCode}`, {
      replace: true,
    });
  };

  const handleCreateFormError = ({ message }: Record<'message', string>) => {
    alert(message);
  };

  const handleStartReview = () => {
    if (
      confirm(
        '템플릿 내용 그대로 회고 답변을 시작하시겠습니까? 계속 진행하면 회고 질문지가 생성됩니다.',
      )
    ) {
      createFormMutation.mutate(Number(templateId), {
        onSuccess: handleCreateSuccess,
        onError: handleCreateFormError,
      });
    }
  };

  return (
    <LayoutContainer>
      <section className={styles.header}>
        <div className={styles.titleContainer}>
          <Text className={styles.title} size={28} weight="bold">
            {template.info.title}
          </Text>
          <div className={styles.info}>
            <TagLabel>
              <>
                <Icon code="download" />
                <span>{`${template.info.usedCount}회`}</span>
              </>
            </TagLabel>
            <div className={styles.iconText}>
              <Icon code="person" />
              <span>{template.creator.nickname}</span>
            </div>
            <div className={styles.iconText}>
              <Icon code="schedule" />
              <span>{getElapsedTimeText(template.info.updatedAt)}</span>
            </div>
          </div>
        </div>
        <div className={styles.buttonContainer}>
          <div className={styles.templateButtons}>
            <Link to={`${PAGE_LIST.TEMPLATE_FORM}?templateId=${template.info.id}`}>
              <Button>
                <Icon code="rate_review" />
                템플릿으로 질문지 만들기
              </Button>
            </Link>
            <Button theme="outlined" onClick={handleStartReview}>
              <Icon code="add_task" />
              템플릿으로 회고하기
            </Button>
          </div>
          {template.isCreator && (
            <div className={styles.iconButtons}>
              <div className={styles.iconButton} onClick={handleDeleteTemplate(template.info.id)}>
                <Icon type="outlined" code="delete" />
                템플릿 삭제
              </div>
              <Link
                to={`${PAGE_LIST.TEMPLATE_FORM}?templateId=${templateId}&templateEditMode=true`}
              >
                <div className={styles.iconButton}>
                  <Icon type="outlined" code="edit" />
                  템플릿 수정
                </div>
              </Link>
            </div>
          )}
        </div>
      </section>
      <section className={styles.contentsContainer}>
        <div className={styles.description}>
          <Text size={18} weight="bold">
            템플릿 소개
          </Text>
          <Text className={styles.text} size={16} weight="lighter">
            {template.info.description}
          </Text>
        </div>
        <Questions>
          {template.questions.map((question, index) => {
            const questionText = `${index + 1}. ${question.value}`;

            return (
              <Questions.Answer key={question.id} question={questionText}>
                {question.description}
              </Questions.Answer>
            );
          })}
        </Questions>
      </section>
      <div className={styles.profileContainer}>
        <Link to={`${PAGE_LIST.USER_PROFILE}/${template.creator.id}`}>
          <SmallProfileCard
            size="large"
            profileUrl={template.creator.profileUrl}
            primaryText={template.creator.nickname}
            secondaryText={template.creator.bio || template.creator.socialNickname || ''}
          />
        </Link>
        <div className={styles.iconContainer}>
          <a
            href={`${GITHUB_PROFILE_URL}${template.creator.socialNickname}`}
            target="_blank"
            rel=" noopener noreferrer"
          >
            <GithubIcon className={styles.icon} />
          </a>
          <Icon className={styles.icon} code="house" type="outlined" />
        </div>
      </div>
      <section className={styles.footer}>
        <section className={styles.headerContainer}>
          <div className={styles.alignCenter}>
            <Icon className={styles.icon} code="local_fire_department" />
            <Text size={18} weight="bold">
              인기 템플릿
            </Text>
          </div>
          <div className={cn(styles.alignCenter, styles.buttonContainer)}>
            <Link to={PAGE_LIST.TEMPLATE_FORM}>
              <Button size="small">템플릿 생성하기</Button>
            </Link>
            <Link to={`${PAGE_LIST.TEMPLATE_LIST}?filter=${TEMPLATE_TAB.TREND}`}>
              <Button size="small" theme="outlined">
                목록으로 돌아가기
              </Button>
            </Link>
          </div>
        </section>
        <section className={styles.trend}>
          <ScrollPanel centerDisabled={true}>
            {trendingTemplates.map((template) => (
              <TemplateCard key={template.info.id} className={styles.card} template={template} />
            ))}
          </ScrollPanel>
        </section>
      </section>
    </LayoutContainer>
  );
}

export default TemplateDetailPage;