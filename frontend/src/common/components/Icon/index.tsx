import cn from 'classnames';

import styles from './styles.module.scss';

interface IconProps extends React.HTMLAttributes<HTMLSpanElement> {
  type?: 'round' | 'outlined' | 'sharp' | 'two-tone';
  code: string;
}

function Icon({ className, type, code, ...rest }: IconProps) {
  return (
    <span className={cn(className, 'icon', styles.icon, `material-icons-${type}`)} {...rest}>
      {code}
    </span>
  );
}

Icon.defaultProps = {
  type: 'round',
};

export default Icon;
