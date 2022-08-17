import { FlexContainer, Icon, Text } from 'common/components';

import Profile from 'service/@shared/components/Profile';

import styles from './styles.module.scss';

interface ContainerProps {
  children: React.ReactNode;
}

function Container({ children }: ContainerProps) {
  return <FlexContainer gap="medium">{children}</FlexContainer>;
}

interface CoverProfileProps {
  image: string;
  title: string;
  description?: string;
}

function CoverProfile({ image, title, description }: CoverProfileProps) {
  return (
    <Profile align="center" textAlign="center" textGap="medium">
      <Profile.Image src={image} />
      <Profile.Nickname size={24}>{title}</Profile.Nickname>
      {description && <Profile.Description>{description}</Profile.Description>}
    </Profile>
  );
}

interface TitleProps {
  children?: string;
}

function Title({}: TitleProps) {
  return (
    <Text size={24} weight="bold" element="div">
      타이틀1
    </Text>
  );
}

interface EditButtonsProps {
  isVisible?: boolean;
  onClickEdit: React.MouseEventHandler<HTMLDivElement>;
  onClickDelete: React.MouseEventHandler<HTMLDivElement>;
}

function EditButtons({ isVisible, onClickEdit, onClickDelete }: EditButtonsProps) {
  if (!isVisible) return null;

  return (
    <FlexContainer className={styles.inlineButtons} direction="row" gap="large" justify="right">
      <FlexContainer className={styles.button} direction="row" align="center" onClick={onClickEdit}>
        <Icon className={styles.icon} code="edit_note" type="round" />
        <Text className={styles.text} size={12}>
          회고 편집
        </Text>
      </FlexContainer>

      <FlexContainer
        className={styles.button}
        direction="row"
        align="center"
        onClick={onClickDelete}
      >
        <Icon className={styles.icon} code="delete" type="round" />
        <Text className={styles.text} size={12}>
          회고 삭제
        </Text>
      </FlexContainer>
    </FlexContainer>
  );
}

interface AnswerProps {
  question: string;
  description?: string;
  children?: React.ReactNode;
}

function Answer({ question, description, children }: AnswerProps) {
  return (
    <FlexContainer className={styles.answerContainer} gap="medium">
      <Text className={styles.question} size={20} weight="bold">
        {question}
      </Text>

      {description && (
        <Text className={styles.description} size={14} weight="lighter">
          {description}
        </Text>
      )}

      <Text className={styles.answer} size={16}>
        {children}
      </Text>
    </FlexContainer>
  );
}

interface ReactionProps {
  onClickLike: React.MouseEventHandler<HTMLDivElement>;
  onClickBookmark: React.MouseEventHandler<HTMLDivElement>;
}

function Reaction({ onClickLike, onClickBookmark }: ReactionProps) {
  return (
    <FlexContainer className={styles.inlineButtons} direction="row" gap="large">
      <FlexContainer className={styles.button} direction="row" align="center" onClick={onClickLike}>
        <Icon className={styles.icon} code="favorite" type="round" />
        <Text className={styles.text} size={12}>
          좋아요
        </Text>
      </FlexContainer>

      <FlexContainer
        className={styles.button}
        direction="row"
        align="center"
        onClick={onClickBookmark}
      >
        <Icon className={styles.icon} code="bookmark" type="round" />
        <Text className={styles.text} size={12}>
          북마크
        </Text>
      </FlexContainer>
    </FlexContainer>
  );
}

const Questions = Object.assign(Container, {
  CoverProfile,
  Title,
  EditButtons,
  Answer,
  Reaction,
});

export default Questions;
